#!/bin/bash
# Script de diagnóstico para verificar el estado del AlphaBot2

echo "========================================="
echo "  Diagnóstico AlphaBot2"
echo "========================================="
echo ""

echo "1. Verificando procesos Python..."
ps aux | grep python | grep -v grep

echo ""
echo "2. Verificando puertos en uso..."
sudo netstat -tulpn | grep -E '5555|8080'

echo ""
echo "3. Verificando logs del servidor de control..."
if [ -f /tmp/joystick_server.log ]; then
    echo "Últimas 10 líneas de joystick_server.log:"
    tail -n 10 /tmp/joystick_server.log
else
    echo "No existe el archivo de log del servidor de control"
fi

echo ""
echo "4. Verificando logs del servidor de streaming..."
if [ -f /tmp/camera_stream.log ]; then
    echo "Últimas 10 líneas de camera_stream.log:"
    tail -n 10 /tmp/camera_stream.log
else
    echo "No existe el archivo de log del streaming"
fi

echo ""
echo "5. Verificando cámara..."
vcgencmd get_camera

echo ""
echo "6. Verificando I2C..."
sudo i2cdetect -y 1

echo ""
echo "========================================="
echo "  Fin del diagnóstico"
echo "========================================="

