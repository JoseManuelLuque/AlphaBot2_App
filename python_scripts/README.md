# Scripts Python para AlphaBot2

Este directorio contiene los scripts Python que se ejecutan en la Raspberry Pi para controlar el AlphaBot2.

## 📁 Archivos

- `joystick_server.py` - Servidor de control de motores y servos
- `joystick_control.py` - Cliente de prueba para enviar comandos al servidor
- `camera_control.py` - Controlador de servos para la cámara
- `camera_stream.py` - Servidor de streaming de video en tiempo real
- `sync_to_raspberry.ps1` - Script para sincronizar archivos a la Raspberry Pi

## 🔧 Instalación de dependencias en Raspberry Pi

Antes de ejecutar los scripts, necesitas instalar las dependencias en tu Raspberry Pi:

```bash
# Actualizar el sistema
sudo apt-get update
sudo apt-get upgrade -y

# Instalar Python 3 y pip (si no están instalados)
sudo apt-get install -y python3 python3-pip

# Instalar biblioteca de GPIO
sudo apt-get install -y python3-rpi.gpio

# Instalar biblioteca de la cámara
sudo apt-get install -y python3-picamera

# Instalar biblioteca I2C para los servos (PCA9685)
sudo apt-get install -y python3-smbus i2c-tools

# Habilitar interfaces necesarias
sudo raspi-config nonint do_camera 0   # Habilitar cámara
sudo raspi-config nonint do_i2c 0      # Habilitar I2C
```

## 🚀 Sincronización de archivos

Para copiar los scripts a la Raspberry Pi, ejecuta desde PowerShell:

```powershell
cd C:\Users\josem\Escritorio\AlphaBot2\python_scripts
.\sync_to_raspberry.ps1
```

**Nota:** Asegúrate de configurar la IP correcta de tu Raspberry Pi en el archivo `sync_to_raspberry.ps1` (línea 7).

## 📡 Puertos utilizados

- **5555** - Servidor de control (motores y servos)
- **8080** - Servidor de streaming de video

## 🎥 Streaming de video

El servidor de streaming usa la cámara de la Raspberry Pi y transmite video en formato MJPEG sobre HTTP.

### Configuración de cámara

Si necesitas ajustar la orientación de la cámara, edita `camera_stream.py`:

```python
camera.rotation = 0      # Rotar imagen (0, 90, 180, 270)
camera.hflip = False     # Voltear horizontalmente
camera.vflip = False     # Voltear verticalmente
```

### Ajustar calidad y resolución

En `camera_stream.py`, puedes modificar:

```python
STREAM_WIDTH = 640       # Ancho en píxeles (640, 800, 1024)
STREAM_HEIGHT = 480      # Alto en píxeles (480, 600, 768)
STREAM_FPS = 20          # Frames por segundo (10-30)
STREAM_QUALITY = 75      # Calidad JPEG (0-100, más alto = mejor calidad)
```

**Nota:** Resoluciones más altas requieren más ancho de banda y procesamiento.

## 🎮 Control de servos de cámara

Los servos están configurados en los canales del PCA9685:
- **Canal 0** - Servo horizontal (izquierda-derecha)
- **Canal 1** - Servo vertical (arriba-abajo)

### Ajustar sensibilidad

En `camera_control.py`, puedes cambiar la velocidad de movimiento:

```python
SPEED_FACTOR = 25  # Microsegundos por actualización
                   # Valores menores = más lento
                   # Valores mayores = más rápido
```

## 🐛 Solución de problemas

### La cámara no funciona
```bash
# Verificar que la cámara esté habilitada
vcgencmd get_camera

# Debería mostrar: supported=1 detected=1

# Verificar logs
cat /tmp/camera_stream.log
```

### Los servos no se mueven
```bash
# Verificar que I2C esté habilitado
sudo i2cdetect -y 1

# Debería mostrar el dispositivo en la dirección 0x40
```

### El servidor no se inicia
```bash
# Ver logs del servidor
cat /tmp/joystick_server.log

# Matar procesos anteriores
pkill -f joystick_server.py
pkill -f camera_stream.py

# Verificar puertos en uso
sudo netstat -tulpn | grep -E '5555|8080'
```

## 📝 Ejecución manual (para pruebas)

```bash
# Servidor de streaming (terminal 1)
python3 '/home/pi/Android App/camera_stream.py'

# Servidor de control (terminal 2)
python3 '/home/pi/Android App/joystick_server.py'

# Ver stream desde navegador
# http://<IP_RASPBERRY>:8080/stream.mjpg
```

## 🔒 Seguridad

- El servidor solo acepta conexiones en la red local
- No uses estas configuraciones en redes públicas
- Considera agregar autenticación si es necesario
