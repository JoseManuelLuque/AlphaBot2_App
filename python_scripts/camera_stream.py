#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
SERVIDOR DE STREAMING DE CÁMARA PARA ALPHABOT2
=============================================================================
Este script crea un servidor HTTP que transmite video en tiempo real desde
la Raspberry Pi Camera usando el formato MJPEG (Motion JPEG).

CARACTERÍSTICAS:
- Resolución: 640x480 píxeles
- Frame rate: 30 FPS
- Puerto: 8080
- Formato: MJPEG (cada frame es una imagen JPEG)
- Soporta múltiples clientes simultáneos

USO:
    python3 camera_stream.py

ACCESO AL STREAM:
    http://<IP_RASPBERRY>:8080/stream.mjpg

AUTOR: José Manuel Luque González
FECHA: 2025
=============================================================================
"""

import io
import socketserver
from threading import Condition
from http import server
import picamera
import time

# =============================================================================
# CONFIGURACIÓN DEL SERVIDOR DE STREAMING
# =============================================================================

STREAM_PORT = 8080          # Puerto HTTP donde se servirá el stream
STREAM_WIDTH = 640          # Ancho del video en píxeles
STREAM_HEIGHT = 480         # Alto del video en píxeles
STREAM_FPS = 30             # Frames por segundo (aprovechando Camera v2)
STREAM_QUALITY = 60         # Calidad JPEG (0-100, menor = más compresión)


# =============================================================================
# CLASE: StreamingOutput
# =============================================================================
class StreamingOutput(object):
    """
    Buffer circular para almacenar frames de video de forma thread-safe.

    Esta clase actúa como un puente entre la cámara (que genera frames) y
    el servidor HTTP (que los sirve a los clientes). Usa un Condition para
    sincronizar el acceso entre threads.

    Funcionamiento:
    1. La cámara escribe frames en el buffer continuamente
    2. Cuando detecta un nuevo frame (empieza con JPEG header 0xFF 0xD8),
       notifica a todos los clientes esperando
    3. Los clientes copian el frame y lo envían a través de HTTP
    """

    def __init__(self):
        """Inicializa el buffer y los mecanismos de sincronización"""
        self.frame = None                    # Último frame capturado (bytes JPEG)
        self.buffer = io.BytesIO()           # Buffer temporal para construir el frame
        self.condition = Condition()         # Lock para sincronización entre threads

    def write(self, buf):
        """
        Método llamado por la cámara cada vez que hay datos nuevos.

        Args:
            buf (bytes): Datos del frame (puede ser fragmentado)

        Returns:
            int: Número de bytes escritos

        Nota:
            Este método es llamado automáticamente por picamera.
            Detecta el inicio de un nuevo frame JPEG por el header 0xFF 0xD8
        """
        # Detectar inicio de nuevo frame JPEG (magic bytes)
        if buf.startswith(b'\xff\xd8'):
            # Frame completo anterior, guardarlo y notificar a los clientes
            self.buffer.truncate()           # Limpiar buffer temporal
            with self.condition:             # Adquirir lock
                self.frame = self.buffer.getvalue()  # Copiar frame completo
                self.condition.notify_all()  # Despertar a todos los clientes esperando
            self.buffer.seek(0)              # Reiniciar buffer para el nuevo frame

        # Escribir los datos recibidos al buffer temporal
        return self.buffer.write(buf)


# =============================================================================
# CLASE: StreamingHandler
# =============================================================================
class StreamingHandler(server.BaseHTTPRequestHandler):
    """
    Manejador de peticiones HTTP para servir el stream de video.

    Rutas disponibles:
    - GET / : Redirige a /stream.mjpg
    - GET /stream.mjpg : Stream MJPEG continuo
    """

    def do_GET(self):
        """
        Maneja las peticiones GET del cliente.

        Implementa dos rutas:
        1. Raíz (/) - Redirige al stream
        2. /stream.mjpg - Sirve el stream MJPEG continuo
        """
        if self.path == '/':
            # ===== RUTA RAÍZ: Redirección =====
            self.send_response(301)                           # Código 301: Moved Permanently
            self.send_header('Location', '/stream.mjpg')      # Redirigir al stream
            self.end_headers()

        elif self.path == '/stream.mjpg':
            # ===== RUTA STREAM: Transmisión continua MJPEG =====

            # Enviar headers HTTP para stream MJPEG
            self.send_response(200)                           # Código 200: OK
            self.send_header('Age', 0)                        # No cachear
            self.send_header('Cache-Control', 'no-cache, private')  # Deshabilitar caché
            self.send_header('Pragma', 'no-cache')            # HTTP/1.0 no-cache
            self.send_header('Content-Type', 'multipart/x-mixed-replace; boundary=FRAME')
            # ↑ Esto indica que es un stream que reemplaza contenido continuamente
            self.end_headers()

            try:
                # Bucle infinito: enviar frames continuamente
                while True:
                    # Esperar a que haya un nuevo frame disponible
                    with output.condition:
                        output.condition.wait()     # Bloquea hasta que hay un nuevo frame
                        frame = output.frame        # Copiar el frame actual

                    # Enviar el frame en formato MJPEG multipart
                    self.wfile.write(b'--FRAME\r\n')                            # Separador multipart
                    self.send_header('Content-Type', 'image/jpeg')             # Tipo de contenido
                    self.send_header('Content-Length', len(frame))             # Tamaño del frame
                    self.end_headers()
                    self.wfile.write(frame)                                     # Datos JPEG
                    self.wfile.write(b'\r\n')                                   # Fin de parte

            except Exception as e:
                # Cliente desconectado o error de red
                print(f'🔌 Cliente desconectado del stream: {e}')

        else:
            # ===== RUTA NO ENCONTRADA =====
            self.send_error(404)        # Código 404: Not Found
            self.end_headers()

    def log_message(self, format, *args):
        """
        Sobrescribe el método de logging para silenciar peticiones HTTP.

        Por defecto, BaseHTTPRequestHandler imprime cada petición en la consola,
        lo cual genera mucho spam. Este método vacío deshabilita ese comportamiento.
        """
        return  # No hacer nada = no imprimir logs


# =============================================================================
# CLASE: StreamingServer
# =============================================================================
class StreamingServer(socketserver.ThreadingMixIn, server.HTTPServer):
    """
    Servidor HTTP con soporte para múltiples clientes simultáneos.

    ThreadingMixIn: Crea un thread nuevo para cada cliente conectado
    HTTPServer: Servidor HTTP básico

    Configuración:
    - allow_reuse_address: Permite reusar el puerto inmediatamente después de cerrar
    - daemon_threads: Los threads de clientes se cierran automáticamente al salir
    """
    allow_reuse_address = True      # Evita error "Address already in use"
    daemon_threads = True            # Threads daemon (se cierran con el programa)


# =============================================================================
# FUNCIÓN PRINCIPAL: start_camera_stream
# =============================================================================
def start_camera_stream():
    """
    Inicializa la cámara y arranca el servidor de streaming.

    Proceso:
    1. Configura la Raspberry Pi Camera
    2. Inicia la captura de video en formato MJPEG
    3. Arranca el servidor HTTP
    4. Mantiene el servidor corriendo hasta Ctrl+C

    Excepciones:
    - Si la cámara no está conectada o habilitada, muestra error y sale
    - Ctrl+C detiene el servidor limpiamente
    """
    global output  # Variable global para que StreamingHandler pueda acceder

    # ===== MENSAJES DE INICIO =====
    print("=" * 60)
    print("🎥 INICIANDO SERVIDOR DE STREAMING DE CÁMARA")
    print("=" * 60)
    print(f"📡 Puerto HTTP:    {STREAM_PORT}")
    print(f"🎬 Resolución:     {STREAM_WIDTH}x{STREAM_HEIGHT}")
    print(f"🎞️  Frame rate:     {STREAM_FPS} FPS")
    print(f"📊 Calidad JPEG:   {STREAM_QUALITY}%")
    print("=" * 60)

    try:
        # ===== INICIALIZAR CÁMARA =====
        print("\n📷 Inicializando Raspberry Pi Camera v2...")
        with picamera.PiCamera(
            resolution=f'{STREAM_WIDTH}x{STREAM_HEIGHT}',
            framerate=STREAM_FPS
        ) as camera:

            # ===== CONFIGURACIÓN DE CÁMARA =====
            # Rotación de la imagen (si la cámara está montada en otro ángulo)
            camera.rotation = 0         # 0, 90, 180 o 270 grados

            # Espejos (descomenta si necesitas voltear la imagen)
            # camera.hflip = True       # Voltear horizontalmente
            # camera.vflip = True       # Voltear verticalmente

            # ===== CREAR BUFFER DE SALIDA =====
            output = StreamingOutput()
            
            # ===== CALIBRACIÓN DE CÁMARA =====
            # Dar tiempo a la cámara para ajustar exposición, balance de blancos, etc.
            print("⏳ Esperando calibración automática de cámara...")
            time.sleep(2)
            
            # ===== INICIAR CAPTURA DE VIDEO =====
            print("🎬 Iniciando captura de video...")
            camera.start_recording(
                output,                     # Destino (nuestro buffer)
                format='mjpeg',             # Formato MJPEG
                quality=STREAM_QUALITY      # Calidad de compresión JPEG
            )

            # ===== INICIAR SERVIDOR HTTP =====
            print("🌐 Iniciando servidor HTTP...")
            address = ('', STREAM_PORT)                         # Escuchar en todas las interfaces
            server = StreamingServer(address, StreamingHandler)
            
            # ===== SERVIDOR ACTIVO =====
            print("\n" + "=" * 60)
            print("✅ SERVIDOR DE STREAMING ACTIVO")
            print("=" * 60)
            print(f"🌐 URL del stream: http://<IP_RASPBERRY>:{STREAM_PORT}/stream.mjpg")
            print("📱 Abre esta URL en tu navegador o app Android")
            print("\n⏹️  Presiona Ctrl+C para detener el servidor")
            print("=" * 60 + "\n")

            try:
                # Mantener el servidor corriendo indefinidamente
                server.serve_forever()
            finally:
                # Limpieza: detener la grabación al salir
                camera.stop_recording()
                print("\n🛑 Grabación detenida")

    except Exception as e:
        # ===== MANEJO DE ERRORES =====
        print("\n" + "=" * 60)
        print("❌ ERROR AL INICIAR EL STREAMING")
        print("=" * 60)
        print(f"Error: {e}")
        print("\n💡 POSIBLES SOLUCIONES:")
        print("   1. Verifica que la cámara esté conectada correctamente al puerto CSI")
        print("   2. Habilita la cámara en raspi-config:")
        print("      sudo raspi-config")
        print("      → Interface Options → Camera → Enable")
        print("   3. Instala la librería picamera:")
        print("      sudo apt-get update")
        print("      sudo apt-get install python3-picamera")
        print("   4. Reinicia la Raspberry Pi después de habilitar la cámara")
        print("=" * 60)


# =============================================================================
# PUNTO DE ENTRADA DEL SCRIPT
# =============================================================================
if __name__ == '__main__':
    """
    Ejecuta el servidor cuando se llama el script directamente.
    
    Maneja interrupciones de teclado (Ctrl+C) para salir limpiamente.
    """
    try:
        start_camera_stream()
    except KeyboardInterrupt:
        # Usuario presionó Ctrl+C
        print("\n\n⛔ SERVIDOR DETENIDO POR USUARIO")
        print("👋 Hasta luego!")
    except Exception as e:
        # Error fatal inesperado
        print(f"\n\n💥 ERROR FATAL: {e}")
        import traceback
        traceback.print_exc()
