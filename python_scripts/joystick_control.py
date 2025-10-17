#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script legacy de control del AlphaBot2 por comandos SSH individuales
⚠️  DEPRECATED - Ya no se usa en la app actual
⚠️  Mantenido solo como referencia histórica

La app ahora usa joystick_server.py que mantiene conexión persistente
y elimina los micro-cortes del sistema antiguo SSH.
"""

import sys
import RPi.GPIO as GPIO
import time

# Configuración de pines del AlphaBot2
AIN1 = 12
AIN2 = 13
PWMA = 6
BIN1 = 20
BIN2 = 21
PWMB = 26

# Configurar GPIO
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
    """Controla el motor izquierdo"""
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
    """Controla el motor derecho"""
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
    """Convierte coordenadas del joystick a velocidades de motores"""
    forward = y * 100
    turn = x * 100

    left = forward + turn
    right = forward - turn

    left = max(-100, min(100, left))
    right = max(-100, min(100, right))

    return int(left), int(right)

def stop():
    """Detiene ambos motores"""
    set_motor_left(0)
    set_motor_right(0)

def cleanup():
    """Limpia recursos GPIO"""
    stop()
    pwm_a.stop()
    pwm_b.stop()
    GPIO.cleanup()

if __name__ == "__main__":
    try:
        # Leer argumentos de línea de comandos
        if len(sys.argv) != 3:
            print("Uso: python3 joystick_control.py <x> <y>")
            sys.exit(1)

        x = float(sys.argv[1])
        y = float(sys.argv[2])

        # Validar rango
        x = max(-1.0, min(1.0, x))
        y = max(-1.0, min(1.0, y))

        # Convertir a velocidades de motor
        left_speed, right_speed = joystick_to_motors(x, y)

        # Aplicar velocidades
        set_motor_left(left_speed)
        set_motor_right(right_speed)

        # Mantener el comando brevemente
        time.sleep(0.05)

    except KeyboardInterrupt:
        pass
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
    finally:
        # No hacer cleanup para mantener estado entre comandos
        pass

