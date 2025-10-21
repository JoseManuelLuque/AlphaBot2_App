#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Servidor de streaming de cámara para AlphaBot2
Compatible con Raspberry Pi Camera usando picamera
"""

import io
import socketserver
from threading import Condition
from http import server
import time
import subprocess
import os
import signal

try:
    import picamera
except ImportError:
    print("ERROR: picamera no instalado")
    print("Instala con: sudo apt-get install python3-picamera")
    exit(1)

STREAM_PORT = 8080
STREAM_WIDTH = 640
STREAM_HEIGHT = 480
STREAM_FPS = 30
STREAM_QUALITY = 70

class StreamingOutput(object):
    def __init__(self):
        self.frame = None
        self.buffer = io.BytesIO()
        self.condition = Condition()

    def write(self, buf):
        if buf.startswith(b'\xff\xd8'):
            self.buffer.truncate()
            with self.condition:
                self.frame = self.buffer.getvalue()
                self.condition.notify_all()
            self.buffer.seek(0)
        return self.buffer.write(buf)

class StreamingHandler(server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.send_response(301)
            self.send_header('Location', '/stream.mjpg')
            self.end_headers()
        elif self.path == '/stream.mjpg':
            self.send_response(200)
            self.send_header('Age', 0)
            self.send_header('Cache-Control', 'no-cache, private')
            self.send_header('Pragma', 'no-cache')
            self.send_header('Content-Type', 'multipart/x-mixed-replace; boundary=FRAME')
            self.end_headers()
            try:
                while True:
                    with output.condition:
                        output.condition.wait()
                        frame = output.frame
                    self.wfile.write(b'--FRAME\r\n')
                    self.send_header('Content-Type', 'image/jpeg')
                    self.send_header('Content-Length', len(frame))
                    self.end_headers()
                    self.wfile.write(frame)
                    self.wfile.write(b'\r\n')
            except Exception as e:
                print(f'Cliente desconectado: {e}')
        else:
            self.send_error(404)
            self.end_headers()

    def log_message(self, format, *args):
        return

class StreamingServer(socketserver.ThreadingMixIn, server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True

def kill_camera_processes():
    """
    Mata cualquier proceso anterior que esté usando la cámara.
    El error 'Out of resources' a menudo significa que otro proceso tiene la cámara abierta.
    """
    print("🔍 Verificando procesos que usan la cámara...")

    # Buscar procesos que usan picamera o la cámara
    try:
        # Obtener PID del proceso actual para NO matarlo
        current_pid = os.getpid()

        # Buscar procesos camera_stream.py que NO sean el actual
        result = subprocess.run(
            ["pgrep", "-f", "camera_stream.py"],
            capture_output=True,
            text=True
        )

        if result.stdout.strip():
            pids = result.stdout.strip().split('\n')
            for pid in pids:
                pid = pid.strip()
                if pid and int(pid) != current_pid:
                    print(f"   → Matando proceso antiguo (PID: {pid})")
                    subprocess.run(["kill", "-9", pid], stderr=subprocess.DEVNULL)
            time.sleep(0.5)

        print("   ✅ Recursos de cámara liberados")

    except Exception as e:
        print(f"   ⚠️  No se pudo verificar/liberar cámara: {e}")
        print("   → Continuando de todos modos...")

def main():
    global output
    camera = None

    print("=" * 60)
    print("SERVIDOR DE STREAMING - ALPHABOT2")
    print("=" * 60)
    print(f"Puerto:     {STREAM_PORT}")
    print(f"Resolucion: {STREAM_WIDTH}x{STREAM_HEIGHT}")
    print(f"FPS:        {STREAM_FPS}")
    print("=" * 60)

    # IMPORTANTE: Liberar recursos de cámara antes de iniciar
    kill_camera_processes()

    try:
        print("\n📷 Iniciando cámara...")

        # ### CAMBIO 1: Simplificar la inicialización ###
        # A menudo, la forma más directa es la mejor.
        # En lugar de usar modos de sensor complejos, inicializamos con los parámetros finales.
        # Si esto falla, las excepciones nos dirán por qué.
        camera = picamera.PiCamera(
            resolution=(STREAM_WIDTH, STREAM_HEIGHT),
            framerate=STREAM_FPS
        )

        # ### CAMBIO 2: Dar tiempo al sensor para que se "despierte" ###
        # Esto es muy importante. A veces la cámara necesita un momento para la calibración
        # automática de exposición y balance de blancos.
        print("   → Calibrando sensor (2 segundos)...")
        time.sleep(2)
        print("   ✅ Cámara inicializada y calibrada.")

        output = StreamingOutput()

        print("\nIniciando captura de video...")
        camera.start_recording(output, format='mjpeg', quality=STREAM_QUALITY)

        try:
            address = ('', STREAM_PORT)
            server_instance = StreamingServer(address, StreamingHandler)

            print(f"\nServidor iniciado correctamente")
            # Obtenemos la IP local para mostrarla
            ip_addr = subprocess.check_output(['hostname', '-I']).decode('utf-8').strip()
            print(f"URL: http://{ip_addr}:{STREAM_PORT}/stream.mjpg")
            print("\nServidor corriendo...\n")

            server_instance.serve_forever()

        finally:
            print("\nDeteniendo grabación y cerrando cámara...")
            camera.stop_recording()
            camera.close()
            print("Cámara cerrada correctamente.")

    except picamera.exc.PiCameraMMALError as e:
        print("\n" + "="*20 + " ERROR DE CÁMARA " + "="*20)
        print("   No se pudo inicializar la cámara (MMALError). Causas comunes:")
        print("   1. Memoria de GPU insuficiente. Ejecuta 'sudo raspi-config', ve a 'Performance Options' -> 'GPU Memory' y asegúrate de que es al menos 128.")
        print("   2. Otro programa está usando la cámara (aunque kill_camera_processes debería haberlo solucionado).")
        print(f"   Detalle del error: {e}")
        print("="*61 + "\n")

    except OSError as e:
        if e.errno == 98:
            print(f"\nERROR: Puerto {STREAM_PORT} en uso")
            print("Ejecuta: sudo pkill -f camera_stream.py")
        else:
            print(f"\nERROR de Sistema Operativo: {e}")
    except Exception as e:
        print(f"\nERROR Inesperado: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()