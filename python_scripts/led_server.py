#!/usr/bin/env python3
"""
Servidor de control de LEDs para AlphaBot2 - CON ANIMACIONES
Controla 4 LEDs WS2812 (NeoPixel) conectados al GPIO 18
Soporta: estático, arcoíris, parpadeo, respiración
"""

import socket
import time
import threading
from rpi_ws281x import Adafruit_NeoPixel, Color

# Configuración de LEDs
LED_COUNT = 4
LED_PIN = 18
LED_FREQ_HZ = 800000
LED_DMA = 10
LED_BRIGHTNESS = 255
LED_INVERT = False
LED_CHANNEL = 0

# Estado global
led_state = {
    'on': False,
    'red': 255,
    'green': 255,
    'blue': 255,
    'brightness': 100,
    'effect': 'static'
}

effect_running = False

strip = Adafruit_NeoPixel(LED_COUNT, LED_PIN, LED_FREQ_HZ, LED_DMA, LED_INVERT, LED_BRIGHTNESS, LED_CHANNEL)
strip.begin()

def wheel(pos):
    """Generar colores del arcoíris"""
    if pos < 85:
        return Color(pos * 3, 255 - pos * 3, 0)
    elif pos < 170:
        pos -= 85
        return Color(255 - pos * 3, 0, pos * 3)
    else:
        pos -= 170
        return Color(0, pos * 3, 255 - pos * 3)

def set_all_leds(color):
    for i in range(strip.numPixels()):
        strip.setPixelColor(i, color)
    strip.show()

def rainbow_cycle():
    j = 0
    while led_state['effect'] == 'rainbow' and led_state['on']:
        for i in range(strip.numPixels()):
            strip.setPixelColor(i, wheel((int(i * 256 / strip.numPixels()) + j) & 255))
        strip.show()
        time.sleep(0.02)
        j = (j + 1) % 256

def blink_effect():
    while led_state['effect'] == 'blink' and led_state['on']:
        brightness_factor = led_state['brightness'] / 100.0
        r = int(led_state['red'] * brightness_factor)
        g = int(led_state['green'] * brightness_factor)
        b = int(led_state['blue'] * brightness_factor)
        set_all_leds(Color(r, g, b))
        time.sleep(0.5)
        set_all_leds(Color(0, 0, 0))
        time.sleep(0.5)

def breathe_effect():
    step = 0
    direction = 1
    while led_state['effect'] == 'breathe' and led_state['on']:
        brightness_factor = (led_state['brightness'] / 100.0) * (step / 100.0)
        r = int(led_state['red'] * brightness_factor)
        g = int(led_state['green'] * brightness_factor)
        b = int(led_state['blue'] * brightness_factor)
        set_all_leds(Color(r, g, b))
        time.sleep(0.02)
        step += direction * 2
        if step >= 100:
            direction = -1
        elif step <= 0:
            direction = 1

def update_leds():
    global effect_running
    if led_state['on']:
        if led_state['effect'] == 'static':
            brightness_factor = led_state['brightness'] / 100.0
            r = int(led_state['red'] * brightness_factor)
            g = int(led_state['green'] * brightness_factor)
            b = int(led_state['blue'] * brightness_factor)
            set_all_leds(Color(r, g, b))
        else:
            if not effect_running:
                effect_running = True
                if led_state['effect'] == 'rainbow':
                    threading.Thread(target=rainbow_cycle, daemon=True).start()
                elif led_state['effect'] == 'blink':
                    threading.Thread(target=blink_effect, daemon=True).start()
                elif led_state['effect'] == 'breathe':
                    threading.Thread(target=breathe_effect, daemon=True).start()
    else:
        effect_running = False
        set_all_leds(Color(0, 0, 0))

def handle_client(conn, addr):
    try:
        while True:
            data = conn.recv(1024).decode('utf-8').strip()
            if not data:
                break
            parts = data.split()
            command = parts[0].upper()

            if command == "ON":
                led_state['on'] = True
                update_leds()
                conn.sendall(b"OK\n")
            elif command == "OFF":
                led_state['on'] = False
                led_state['effect'] = 'static'
                update_leds()
                conn.sendall(b"OK\n")
            elif command == "COLOR" and len(parts) == 4:
                led_state['red'] = int(parts[1])
                led_state['green'] = int(parts[2])
                led_state['blue'] = int(parts[3])
                led_state['effect'] = 'static'
                update_leds()
                conn.sendall(b"OK\n")
            elif command == "BRIGHTNESS" and len(parts) == 2:
                led_state['brightness'] = int(parts[1])
                update_leds()
                conn.sendall(b"OK\n")
            elif command == "EFFECT" and len(parts) == 2:
                effect = parts[1].lower()
                if effect in ['static', 'rainbow', 'blink', 'breathe']:
                    led_state['effect'] = effect
                    update_leds()
                    conn.sendall(b"OK\n")
                else:
                    conn.sendall(b"ERROR\n")
            elif command == "QUIT":
                conn.sendall(b"BYE\n")
                break
            else:
                conn.sendall(b"ERROR\n")
    except:
        pass
    finally:
        conn.close()

def main():
    HOST = '0.0.0.0'
    PORT = 5556
    set_all_leds(Color(0, 0, 0))
    print("🌈 Servidor de LEDs iniciado en puerto", PORT)

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        while True:
            conn, addr = s.accept()
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()

if __name__ == "__main__":
    main()

