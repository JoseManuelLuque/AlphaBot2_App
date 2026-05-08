import time

import RPi.GPIO as GPIO

# Pines correctos según Infrared_Obstacle_Avoidance.py
DR = 16  # Sensor DERECHO
DL = 19  # Sensor IZQUIERDO

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
GPIO.setup(DR, GPIO.IN, GPIO.PUD_UP)
GPIO.setup(DL, GPIO.IN, GPIO.PUD_UP)

try:
    print("Probando sensores ST188 - Presiona Ctrl+C para salir")
    print("0 = Obstáculo detectado | 1 = Sin obstáculo")
    print("-" * 50)

    while True:
        DR_status = GPIO.input(DR)
        DL_status = GPIO.input(DL)

        print(f"Izquierdo (pin 19): {DL_status} | Derecho (pin 16): {DR_status}")

        if DL_status == 0:
            print("  ⚠️  Obstáculo detectado a la IZQUIERDA")
        if DR_status == 0:
            print("  ⚠️  Obstáculo detectado a la DERECHA")

        time.sleep(0.5)

except KeyboardInterrupt:
    print("\nPrueba finalizada")
finally:
    GPIO.cleanup()
