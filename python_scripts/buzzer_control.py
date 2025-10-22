#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
==================================================
CONTROL DEL BUZZER - ALPHABOT2
==================================================
Script para reproducir pitidos y melodías en el buzzer
del AlphaBot2 usando GPIO
==================================================
"""

import RPi.GPIO as GPIO
import time
import sys

# Configuración del buzzer
BUZZER_PIN = 4  # GPIO 4 (Pin 7)

# Configuración de GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
GPIO.setup(BUZZER_PIN, GPIO.OUT)

# Notas musicales (frecuencias en Hz)
NOTES = {
    'C4': 262, 'D4': 294, 'E4': 330, 'F4': 349, 'G4': 392, 'A4': 440, 'B4': 494,
    'C5': 523, 'D5': 587, 'E5': 659, 'F5': 698, 'G5': 784, 'A5': 880, 'B5': 988,
    'C6': 1047, 'D6': 1175, 'E6': 1319, 'F6': 1397, 'G6': 1568, 'A6': 1760, 'B6': 1976,
    'REST': 0
}

def beep(frequency, duration):
    """Reproduce un tono en el buzzer"""
    if frequency == 0:
        time.sleep(duration)
        return

    pwm = GPIO.PWM(BUZZER_PIN, frequency)
    pwm.start(50)  # 50% duty cycle
    time.sleep(duration)
    pwm.stop()

def play_note(note, duration):
    """Reproduce una nota musical"""
    if note in NOTES:
        beep(NOTES[note], duration)
    else:
        time.sleep(duration)

# ========== PITIDOS SIMPLES ==========

def beep_short():
    """Pitido corto"""
    print("🔊 Pitido corto")
    beep(1000, 0.1)

def beep_long():
    """Pitido largo"""
    print("🔊 Pitido largo")
    beep(1000, 0.5)

def beep_double():
    """Doble pitido"""
    print("🔊 Doble pitido")
    beep(1000, 0.1)
    time.sleep(0.1)
    beep(1000, 0.1)

# ========== MELODÍAS ==========

def song_star_wars():
    """Tema principal de Star Wars"""
    print("🌟 Reproduciendo: Star Wars Theme")
    melody = [
        ('D4', 0.4), ('D4', 0.4), ('D4', 0.4),
        ('G4', 0.3), ('D5', 0.1),
        ('C5', 0.4), ('B4', 0.4), ('A4', 0.4),
        ('G5', 0.3), ('D5', 0.1),
        ('C5', 0.4), ('B4', 0.4), ('A4', 0.4),
        ('G5', 0.3), ('D5', 0.1),
        ('C5', 0.4), ('B4', 0.4), ('C5', 0.4), ('A4', 0.8)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.03)

def song_happy_birthday():
    """Cumpleaños feliz"""
    print("🎂 Reproduciendo: Happy Birthday")
    melody = [
        ('G4', 0.25), ('G4', 0.25), ('A4', 0.5), ('G4', 0.5), ('C5', 0.5), ('B4', 1.0),
        ('G4', 0.25), ('G4', 0.25), ('A4', 0.5), ('G4', 0.5), ('D5', 0.5), ('C5', 1.0),
        ('G4', 0.25), ('G4', 0.25), ('G5', 0.5), ('E5', 0.5), ('C5', 0.5), ('B4', 0.5), ('A4', 0.5),
        ('F5', 0.25), ('F5', 0.25), ('E5', 0.5), ('C5', 0.5), ('D5', 0.5), ('C5', 1.0)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.05)

def song_super_mario():
    """Tema de Super Mario Bros"""
    print("🍄 Reproduciendo: Super Mario Theme")
    melody = [
        ('E5', 0.15), ('E5', 0.15), ('REST', 0.15), ('E5', 0.15),
        ('REST', 0.15), ('C5', 0.15), ('E5', 0.3),
        ('G5', 0.3), ('REST', 0.3), ('G4', 0.3), ('REST', 0.3),
        ('C5', 0.3), ('REST', 0.15), ('G4', 0.3), ('REST', 0.15),
        ('E4', 0.3), ('REST', 0.15), ('A4', 0.3), ('B4', 0.3),
        ('A4', 0.15), ('A4', 0.3)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.02)

def song_take_on_me():
    """A-ha - Take On Me"""
    print("🎸 Reproduciendo: Take On Me")
    melody = [
        ('F5', 0.125), ('F5', 0.125), ('D5', 0.125), ('D5', 0.125),
        ('F5', 0.125), ('F5', 0.125), ('G5', 0.25),
        ('F5', 0.125), ('F5', 0.125), ('D5', 0.125), ('D5', 0.125),
        ('F5', 0.125), ('F5', 0.125), ('C6', 0.25),
        ('C6', 0.125), ('A5', 0.125), ('D5', 0.125), ('D5', 0.125),
        ('A5', 0.125), ('A5', 0.125), ('G5', 0.25)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.02)

def song_nokia_ringtone():
    """Clásico tono Nokia"""
    print("📱 Reproduciendo: Nokia Ringtone")
    melody = [
        ('E5', 0.125), ('D5', 0.125), ('F4', 0.25), ('G4', 0.25),
        ('C5', 0.125), ('B4', 0.125), ('D4', 0.25), ('E4', 0.25),
        ('B4', 0.125), ('A4', 0.125), ('C4', 0.25), ('E4', 0.25),
        ('A4', 0.5)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.05)

def song_tetris():
    """Tema de Tetris"""
    print("🎮 Reproduciendo: Tetris Theme")
    melody = [
        ('E5', 0.25), ('B4', 0.125), ('C5', 0.125), ('D5', 0.25),
        ('C5', 0.125), ('B4', 0.125), ('A4', 0.25), ('A4', 0.125),
        ('C5', 0.125), ('E5', 0.25), ('D5', 0.125), ('C5', 0.125),
        ('B4', 0.375), ('C5', 0.125), ('D5', 0.25), ('E5', 0.25),
        ('C5', 0.25), ('A4', 0.25), ('A4', 0.25)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.05)

def song_imperial_march():
    """La Marcha Imperial - Star Wars"""
    print("⚫ Reproduciendo: Imperial March")
    melody = [
        ('A4', 0.5), ('A4', 0.5), ('A4', 0.5),
        ('F4', 0.35), ('C5', 0.15),
        ('A4', 0.5), ('F4', 0.35), ('C5', 0.15), ('A4', 0.8),
        ('REST', 0.2),
        ('E5', 0.5), ('E5', 0.5), ('E5', 0.5),
        ('F5', 0.35), ('C5', 0.15),
        ('A4', 0.5), ('F4', 0.35), ('C5', 0.15), ('A4', 0.8)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.03)

def song_jingle_bells():
    """Jingle Bells"""
    print("🔔 Reproduciendo: Jingle Bells")
    melody = [
        ('E5', 0.25), ('E5', 0.25), ('E5', 0.5),
        ('E5', 0.25), ('E5', 0.25), ('E5', 0.5),
        ('E5', 0.25), ('G5', 0.25), ('C5', 0.375), ('D5', 0.125), ('E5', 1.0),
        ('F5', 0.25), ('F5', 0.25), ('F5', 0.375), ('F5', 0.125),
        ('F5', 0.25), ('E5', 0.25), ('E5', 0.25), ('E5', 0.125), ('E5', 0.125),
        ('E5', 0.25), ('D5', 0.25), ('D5', 0.25), ('E5', 0.25), ('D5', 0.5), ('G5', 0.5)
    ]

    for note, duration in melody:
        play_note(note, duration)
        time.sleep(0.05)

# ========== FUNCIÓN PRINCIPAL ==========

def main():
    if len(sys.argv) < 2:
        print("❌ Error: Se requiere un comando")
        print("Uso: python3 buzzer_control.py <comando>")
        print("\nComandos disponibles:")
        print("  beep_short, beep_long, beep_double")
        print("  song_star_wars, song_happy_birthday, song_super_mario")
        print("  song_take_on_me, song_nokia_ringtone, song_tetris")
        print("  song_imperial_march, song_jingle_bells")
        sys.exit(1)

    command = sys.argv[1]

    try:
        print(f"============================================================")
        print(f"CONTROL DEL BUZZER - ALPHABOT2")
        print(f"============================================================")

        # Ejecutar comando
        if command == "beep_short":
            beep_short()
        elif command == "beep_long":
            beep_long()
        elif command == "beep_double":
            beep_double()
        elif command == "song_star_wars":
            song_star_wars()
        elif command == "song_happy_birthday":
            song_happy_birthday()
        elif command == "song_super_mario":
            song_super_mario()
        elif command == "song_take_on_me":
            song_take_on_me()
        elif command == "song_nokia_ringtone":
            song_nokia_ringtone()
        elif command == "song_tetris":
            song_tetris()
        elif command == "song_imperial_march":
            song_imperial_march()
        elif command == "song_jingle_bells":
            song_jingle_bells()
        else:
            print(f"❌ Comando desconocido: {command}")
            sys.exit(1)

        print(f"✅ Reproducción completada")

    except KeyboardInterrupt:
        print("\n⚠️ Reproducción interrumpida")
    except Exception as e:
        print(f"❌ ERROR: {e}")
        sys.exit(1)
    finally:
        GPIO.cleanup()

if __name__ == "__main__":
    main()

