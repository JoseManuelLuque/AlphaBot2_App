#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Servidor continuo para control del AlphaBot2 mediante joystick
Mantiene los motores inicializados y recibe comandos por socket TCP
Esto elimina completamente los micro-cortes del control SSH

Puerto: 5555
Timeout de seguridad: 500ms
"""

import socket
import RPi.GPIO as GPIO
import sys
import time
import threading

# Configuración de pines del AlphaBot2
AIN1 = 12  # Motor izquierdo - Dirección 1
AIN2 = 13  # Motor izquierdo - Dirección 2
PWMA = 6   # Motor izquierdo - PWM (velocidad)
BIN1 = 20  # Motor derecho - Dirección 1
BIN2 = 21  # Motor derecho - Dirección 2
PWMB = 26  # Motor derecho - PWM (velocidad)

# Variables globales para control de timeout
last_command_time = time.time()
motors_active = False
TIMEOUT_SECONDS = 0.5  # Detener motores si no hay comandos por 500ms

# Inicializar GPIO una sola vez al arrancar
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
GPIO.setup(AIN1, GPIO.OUT)
GPIO.setup(AIN2, GPIO.OUT)
GPIO.setup(BIN1, GPIO.OUT)
GPIO.setup(BIN2, GPIO.OUT)
GPIO.setup(PWMA, GPIO.OUT)
GPIO.setup(PWMB, GPIO.OUT)

# Crear objetos PWM (frecuencia 1000 Hz)
pwm_a = GPIO.PWM(PWMA, 1000)
pwm_b = GPIO.PWM(PWMB, 1000)
pwm_a.start(0)
pwm_b.start(0)

def set_motor_left(speed):
    """
    Controla el motor izquierdo
    Args:
        speed: Velocidad entre -100 y 100 (negativo = reversa)
    """
    if speed > 0:
        GPIO.output(AIN1, GPIO.HIGH)
        GPIO.output(AIN2, GPIO.LOW)
        pwm_a.ChangeDutyCycle(min(abs(speed), 100))
    elif speed < 0:
        GPIO.output(AIN1, GPIO.LOW)
        GPIO.output(AIN2, GPIO.HIGH)
        pwm_a.ChangeDutyCycle(min(abs(speed), 100))
    else:
        GPIO.output(AIN1, GPIO.LOW)
        GPIO.output(AIN2, GPIO.LOW)
        pwm_a.ChangeDutyCycle(0)

def set_motor_right(speed):
    """
    Controla el motor derecho
    Args:
        speed: Velocidad entre -100 y 100 (negativo = reversa)
    """
    if speed > 0:
        GPIO.output(BIN1, GPIO.HIGH)
        GPIO.output(BIN2, GPIO.LOW)
        pwm_b.ChangeDutyCycle(min(abs(speed), 100))
    elif speed < 0:
        GPIO.output(BIN1, GPIO.LOW)
        GPIO.output(BIN2, GPIO.HIGH)
        pwm_b.ChangeDutyCycle(min(abs(speed), 100))
    else:
        GPIO.output(BIN1, GPIO.LOW)
        GPIO.output(BIN2, GPIO.LOW)
        pwm_b.ChangeDutyCycle(0)

def joystick_to_motors(x, y):
    """
    Convierte coordenadas del joystick a velocidades de motores
    Usa mezcla tipo "tank-drive" para control intuitivo

    Args:
        x: Coordenada X del joystick (-1.0 a 1.0)
        y: Coordenada Y del joystick (-1.0 a 1.0)

    Returns:
        Tupla (velocidad_izquierda, velocidad_derecha) entre -100 y 100
    """
    forward = y * 100  # Componente adelante/atrás
    turn = x * 100     # Componente de giro

    # Mezcla tank-drive
    left = forward + turn
    right = forward - turn

    # Limitar a rango válido
    left = max(-100, min(100, left))
    right = max(-100, min(100, right))

    # Zona muerta: si la velocidad es muy baja, ponerla a 0
    # Esto evita vibraciones o movimientos residuales
    if abs(left) < 5:
        left = 0
    if abs(right) < 5:
        right = 0

    return int(left), int(right)

def stop():
    """Detiene ambos motores completamente y asegura que todos los pines estén en LOW"""
    global motors_active
    # Asegurar que los motores están completamente apagados
    GPIO.output(AIN1, GPIO.LOW)
    GPIO.output(AIN2, GPIO.LOW)
    GPIO.output(BIN1, GPIO.LOW)
    GPIO.output(BIN2, GPIO.LOW)
    pwm_a.ChangeDutyCycle(0)
    pwm_b.ChangeDutyCycle(0)
    motors_active = False

def cleanup():
    """Limpia recursos GPIO antes de salir"""
    stop()
    pwm_a.stop()
    pwm_b.stop()
    GPIO.cleanup()

def timeout_watchdog():
    """
    Thread de vigilancia que monitorea timeout y detiene motores automáticamente
    si no hay comandos por TIMEOUT_SECONDS segundos
    """
    global last_command_time, motors_active
    while True:
        time.sleep(0.1)  # Chequear cada 100ms
        if motors_active and (time.time() - last_command_time) > TIMEOUT_SECONDS:
            print("⚠️  Timeout detectado - deteniendo motores por seguridad")
            stop()

def start_server(port=5555):
    """
    Inicia el servidor socket que escucha comandos del joystick

    Args:
        port: Puerto TCP donde escuchar (default: 5555)
    """
    global last_command_time, motors_active

    # Iniciar thread de watchdog de seguridad
    watchdog = threading.Thread(target=timeout_watchdog, daemon=True)
    watchdog.start()

    # Crear socket del servidor
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(('0.0.0.0', port))
    server_socket.listen(1)

    print(f"🚀 Servidor joystick escuchando en puerto {port}...")
    print(f"🛡️  Timeout de seguridad: {TIMEOUT_SECONDS}s")

    try:
        while True:
            # Esperar conexión de cliente
            client_socket, address = server_socket.accept()
            print(f"✅ Conexión establecida desde {address}")

            try:
                while True:
                    # Recibir comando
                    data = client_socket.recv(1024).decode('utf-8').strip()

                    if not data:
                        break

                    # Actualizar timestamp de último comando
                    last_command_time = time.time()

                    # Parsear comando "x y"
                    parts = data.split()
                    if len(parts) == 2:
                        try:
                            x = float(parts[0])
                            y = float(parts[1])

                            # Limitar valores a rango válido
                            x = max(-1.0, min(1.0, x))
                            y = max(-1.0, min(1.0, y))

                            # Convertir a velocidades de motor
                            left_speed, right_speed = joystick_to_motors(x, y)

                            # Aplicar velocidades a los motores
                            set_motor_left(left_speed)
                            set_motor_right(right_speed)
                            motors_active = (left_speed != 0 or right_speed != 0)

                            # Respuesta de confirmación (opcional)
                            client_socket.send(b"OK\n")

                        except ValueError:
                            client_socket.send(b"ERROR: Invalid values\n")

                    elif data.lower() == "stop":
                        # Comando explícito de parada
                        stop()
                        client_socket.send(b"STOPPED\n")

                    elif data.lower() == "quit":
                        # Comando de desconexión
                        stop()
                        client_socket.send(b"BYE\n")
                        break

            except Exception as e:
                print(f"❌ Error con cliente: {e}")
            finally:
                client_socket.close()
                stop()  # Detener motores al desconectar
                print("🔌 Cliente desconectado - motores detenidos")

    except KeyboardInterrupt:
        print("\n⛔ Servidor detenido por usuario")
    finally:
        server_socket.close()
        cleanup()

if __name__ == "__main__":
    try:
        start_server()
    except Exception as e:
        print(f"💥 Error fatal: {e}")
        cleanup()
        sys.exit(1)

