# Scripts Python para AlphaBot2

Esta carpeta contiene todos los scripts Python que deben copiarse a la Raspberry Pi en `/home/pi/Android App/`.

## Scripts disponibles:

### 1. `joystick_server.py`
Servidor principal para control en tiempo real del AlphaBot2 mediante socket.
- Mantiene conexión persistente
- Sin micro-cortes
- Timeout automático de seguridad (500ms)
- Puerto: 5555

### 2. `joystick_control.py` (DEPRECATED)
Script legacy de control por comandos SSH individuales. Ya no se usa.

## Instalación en Raspberry Pi:

```bash
# Copiar todos los scripts
scp C:\Users\josem\Escritorio\AlphaBot2\python_scripts\*.py pi@<IP_RASPBERRY>:"/home/pi/Android App/"

# Dar permisos de ejecución
ssh pi@<IP_RASPBERRY>
chmod +x "/home/pi/Android App/"*.py
```

## Uso:

El servidor se inicia automáticamente cuando abres la app Android.
No necesitas ejecutar nada manualmente en la Raspberry Pi.

