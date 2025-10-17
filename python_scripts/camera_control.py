#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
CONTROLADOR DE SERVOS DE CÁMARA PARA ALPHABOT2
=============================================================================
Este módulo controla los servos que mueven la cámara del robot mediante
el chip PCA9685 (controlador PWM de 16 canales vía I2C).

CARACTERÍSTICAS:
- Control tipo FPS (First Person Shooter) incremental
- 2 servos: horizontal (pan) y vertical (tilt)
- Rango de movimiento: 500-2500 microsegundos
- Comunicación I2C con PCA9685
- Desactivación automática de PWM para evitar ruido

SERVOS:
- Canal 0: Movimiento horizontal (izquierda-derecha)
- Canal 1: Movimiento vertical (arriba-abajo)

CONTROL:
El control es tipo FPS: el joystick indica VELOCIDAD de rotación,
no posición absoluta. Mantener el stick = seguir girando.

AUTOR: José Manuel Luque González
FECHA: 2025
=============================================================================
"""

import time
import math
import smbus

# =============================================================================
# CLASE: PCA9685 (Driver del chip controlador PWM)
# =============================================================================
class PCA9685:
    """
    Driver para el chip PCA9685 de 16 canales PWM vía I2C.

    El PCA9685 es un controlador PWM que permite controlar hasta 16 servos
    mediante el bus I2C. Se usa comúnmente para controlar servos en robots.

    Características:
    - 16 canales PWM independientes
    - Resolución de 12 bits (4096 pasos)
    - Frecuencia PWM configurable
    - Comunicación I2C (dirección por defecto: 0x40)
    """

    # ===== REGISTROS DEL PCA9685 =====
    # Direcciones de los registros internos del chip
    __SUBADR1            = 0x02      # Dirección I2C subalterna 1
    __SUBADR2            = 0x03      # Dirección I2C subalterna 2
    __SUBADR3            = 0x04      # Dirección I2C subalterna 3
    __MODE1              = 0x00      # Registro de modo 1
    __PRESCALE           = 0xFE      # Registro prescaler (frecuencia PWM)
    __LED0_ON_L          = 0x06      # Canal 0 - byte bajo de encendido
    __LED0_ON_H          = 0x07      # Canal 0 - byte alto de encendido
    __LED0_OFF_L         = 0x08      # Canal 0 - byte bajo de apagado
    __LED0_OFF_H         = 0x09      # Canal 0 - byte alto de apagado
    __ALLLED_ON_L        = 0xFA      # Todos los canales - byte bajo ON
    __ALLLED_ON_H        = 0xFB      # Todos los canales - byte alto ON
    __ALLLED_OFF_L       = 0xFC      # Todos los canales - byte bajo OFF
    __ALLLED_OFF_H       = 0xFD      # Todos los canales - byte alto OFF

    def __init__(self, address=0x40, debug=False):
        """
        Inicializa el chip PCA9685.

        Args:
            address (int): Dirección I2C del chip (por defecto 0x40)
            debug (bool): Si True, imprime información de depuración
        """
        self.bus = smbus.SMBus(1)         # Bus I2C 1 (GPIO 2 y 3 en Raspberry Pi)
        self.address = address             # Dirección I2C del chip
        self.debug = debug                 # Modo debug

        if self.debug:
            print(f"🔧 Inicializando PCA9685 en dirección I2C 0x{address:02X}")

        # Reset del chip: escribir 0x00 en MODE1
        self.write(self.__MODE1, 0x00)

    def write(self, reg, value):
        """
        Escribe un byte en un registro del chip.

        Args:
            reg (int): Dirección del registro
            value (int): Valor a escribir (0-255)
        """
        self.bus.write_byte_data(self.address, reg, value)
        if self.debug:
            print(f"I2C Write: 0x{value:02X} → registro 0x{reg:02X}")

    def read(self, reg):
        """
        Lee un byte desde un registro del chip.

        Args:
            reg (int): Dirección del registro

        Returns:
            int: Valor leído (0-255)
        """
        result = self.bus.read_byte_data(self.address, reg)
        if self.debug:
            print(f"I2C Read: 0x{result:02X} ← registro 0x{reg:02X}")
        return result

    def setPWMFreq(self, freq):
        """
        Configura la frecuencia PWM del chip.

        Para servos, la frecuencia estándar es 50 Hz (período de 20ms).

        Args:
            freq (int): Frecuencia deseada en Hz (típicamente 50 para servos)

        Cálculo:
        - Clock interno del PCA9685: 25 MHz
        - Resolución: 12 bits (4096 pasos)
        - Prescaler = (25MHz / (4096 * freq)) - 1
        """
        # Calcular valor del prescaler
        prescaleval = 25000000.0          # Frecuencia del oscilador interno (25 MHz)
        prescaleval /= 4096.0             # Resolución de 12 bits
        prescaleval /= float(freq)        # Dividir por frecuencia deseada
        prescaleval -= 1.0                # Ajuste de fórmula

        if self.debug:
            print(f"⚙️  Configurando frecuencia PWM a {freq} Hz")
            print(f"   Prescaler calculado: {prescaleval}")

        prescale = math.floor(prescaleval + 0.5)  # Redondear

        if self.debug:
            print(f"   Prescaler final: {prescale}")

        # Para cambiar la frecuencia, el chip debe estar en modo sleep
        oldmode = self.read(self.__MODE1)
        newmode = (oldmode & 0x7F) | 0x10        # Modo sleep (bit 4 = 1)
        self.write(self.__MODE1, newmode)        # Entrar en sleep
        self.write(self.__PRESCALE, int(math.floor(prescale)))  # Escribir prescaler
        self.write(self.__MODE1, oldmode)        # Salir de sleep
        time.sleep(0.005)                        # Esperar estabilización
        self.write(self.__MODE1, oldmode | 0x80) # Restart con auto-increment

    def setPWM(self, channel, on, off):
        """
        Configura el ciclo de trabajo PWM de un canal.

        El PWM se controla mediante dos valores de 12 bits:
        - ON: En qué punto del ciclo se enciende la señal (0-4095)
        - OFF: En qué punto del ciclo se apaga la señal (0-4095)

        Args:
            channel (int): Número de canal (0-15)
            on (int): Tick de encendido (0-4095)
            off (int): Tick de apagado (0-4095)

        Ejemplo:
            setPWM(0, 0, 2048) → Canal 0 al 50% (2048/4096)
        """
        # Escribir los 4 bytes del canal (ON_L, ON_H, OFF_L, OFF_H)
        self.write(self.__LED0_ON_L + 4*channel, on & 0xFF)        # Byte bajo ON
        self.write(self.__LED0_ON_H + 4*channel, on >> 8)          # Byte alto ON
        self.write(self.__LED0_OFF_L + 4*channel, off & 0xFF)      # Byte bajo OFF
        self.write(self.__LED0_OFF_H + 4*channel, off >> 8)        # Byte alto OFF

        if self.debug:
            print(f"Canal {channel}: ON={on}, OFF={off}")

    def setServoPulse(self, channel, pulse):
        """
        Configura un servo mediante el ancho de pulso en microsegundos.

        Los servos se controlan con pulsos de 1000-2000 µs (típicamente).
        - 1000 µs: Posición mínima (0°)
        - 1500 µs: Posición central (90°)
        - 2000 µs: Posición máxima (180°)

        Args:
            channel (int): Número de canal del servo (0-15)
            pulse (int): Ancho de pulso en microsegundos

        Conversión:
        - Frecuencia PWM: 50 Hz → Período: 20000 µs
        - Resolución: 4096 pasos
        - Paso = 20000 / 4096 ≈ 4.88 µs
        - Valor = pulse / 4.88
        """
        # Convertir microsegundos a valor de 12 bits
        # Período de 20ms (50 Hz) = 20000 µs
        pulse = int(pulse * 4096 / 20000)
        self.setPWM(channel, 0, pulse)


# =============================================================================
# CLASE: CameraController (Controlador de cámara)
# =============================================================================
class CameraController:
    """
    Controlador para los servos de la cámara con control tipo FPS incremental.

    Este controlador mueve dos servos (horizontal y vertical) para apuntar
    la cámara. El control es tipo FPS (First Person Shooter): el joystick
    indica VELOCIDAD de rotación, no posición absoluta.

    Funcionamiento:
    - Joystick centrado = cámara quieta
    - Joystick a la derecha = cámara GIRA a la derecha continuamente
    - Joystick arriba = cámara SUBE continuamente

    Características:
    - Control incremental tipo FPS
    - Límites de movimiento configurables
    - Desactivación automática de PWM para evitar ruido
    - Centrado automático al inicio
    """

    # ===== CONFIGURACIÓN DE CANALES =====
    SERVO_HORIZONTAL = 0              # Canal PWM para servo horizontal (pan)
    SERVO_VERTICAL = 1                # Canal PWM para servo vertical (tilt)

    # ===== LÍMITES DE PULSO (en microsegundos) =====
    MIN_PULSE = 200                   # Pulso mínimo del servo
    MAX_PULSE = 1600                  # Pulso máximo del servo

    # ===== CALIBRACIÓN DEL CENTRO =====
    # Ajusta estos valores para que la cámara apunte exactamente al frente
    # Si la cámara apunta a la izquierda, AUMENTA CENTER_HORIZONTAL
    # Si la cámara apunta a la derecha, DISMINUYE CENTER_HORIZONTAL
    # Si la cámara apunta arriba, AUMENTA CENTER_VERTICAL
    # Si la cámara apunta abajo, DISMINUYE CENTER_VERTICAL
    CENTER_HORIZONTAL = 900          # Centro horizontal (ajustado para calibración)
    CENTER_VERTICAL = 1100            # Centro vertical (puede necesitar ajuste)

    def __init__(self, debug=False):
        """
        Inicializa el controlador de cámara.

        Args:
            debug (bool): Si True, activa mensajes de depuración

        Proceso:
        1. Inicializa el chip PCA9685
        2. Configura la frecuencia PWM a 50 Hz
        3. Centra ambos servos
        4. Desactiva PWM para evitar ruido
        """
        # ===== INICIALIZAR CHIP PCA9685 =====
        self.pwm = PCA9685(0x40, debug=debug)
        self.pwm.setPWMFreq(50)           # Frecuencia estándar para servos: 50 Hz

        # ===== POSICIONES ACTUALES =====
        # Guardamos la posición actual de cada servo en microsegundos
        self.horizontal_pos = self.CENTER_HORIZONTAL
        self.vertical_pos = self.CENTER_VERTICAL

        # ===== ESTADO DE MOVIMIENTO =====
        self.is_moving = False            # Indica si los servos están en movimiento

        # ===== CENTRAR SERVOS AL INICIO =====
        print("📷 Inicializando controlador de cámara...")
        self.center()
        print("✅ Cámara centrada y lista")

    def center(self):
        """
        Centra ambos servos en su posición media.

        Proceso:
        1. Mueve ambos servos a la posición central (1500 µs)
        2. Espera 300ms para que los servos lleguen a la posición
        3. Desactiva PWM para evitar vibraciones y ruido
        """
        print("🎯 Centrando cámara...")
        self.horizontal_pos = self.CENTER_HORIZONTAL
        self.vertical_pos = self.CENTER_VERTICAL

        # Aplicar posición central a ambos servos
        self.pwm.setServoPulse(self.SERVO_HORIZONTAL, self.horizontal_pos)
        self.pwm.setServoPulse(self.SERVO_VERTICAL, self.vertical_pos)

        # Esperar a que los servos se muevan físicamente
        time.sleep(0.3)

        # Desactivar PWM para evitar ruido (los servos mantienen su posición)
        self.stop_pwm()

    def stop_pwm(self):
        """
        Desactiva la señal PWM de ambos servos.

        Cuando un servo está en posición, no necesita señal PWM constante.
        Mantener PWM activo causa:
        - Ruido/zumbido del servo
        - Vibraciones
        - Consumo innecesario de energía

        Esta función apaga la señal PWM poniendo el canal a 0.
        El servo mantiene su posición por fricción interna.
        """
        self.pwm.setPWM(self.SERVO_HORIZONTAL, 0, 0)  # Apagar canal 0
        self.pwm.setPWM(self.SERVO_VERTICAL, 0, 0)    # Apagar canal 1
        self.is_moving = False

    def move_incremental(self, velocity_x, velocity_y):
        """
        Mueve la cámara de forma incremental basado en velocidad (CONTROL TIPO FPS).

        Este es el método principal para controlar la cámara desde el joystick.
        A diferencia del control absoluto, aquí el joystick indica VELOCIDAD:

        - velocity_x > 0: Cámara GIRA a la derecha (continuamente)
        - velocity_x < 0: Cámara GIRA a la izquierda (continuamente)
        - velocity_y > 0: Cámara SUBE (continuamente)
        - velocity_y < 0: Cámara BAJA (continuamente)
        - Ambos en 0: Cámara se DETIENE en su posición actual

        Args:
            velocity_x (float): Velocidad horizontal (-1.0 a 1.0)
                                -1.0 = girar izquierda rápido
                                 0.0 = no girar
                                +1.0 = girar derecha rápido

            velocity_y (float): Velocidad vertical (-1.0 a 1.0)
                                -1.0 = bajar rápido
                                 0.0 = no subir/bajar
                                +1.0 = subir rápido

        Comportamiento:
        - Si no hay velocidad (joystick centrado), apaga PWM y mantiene posición
        - Si hay velocidad, incrementa la posición actual según la velocidad
        - Limita la posición a los rangos válidos del servo
        """

        # ===== ZONA MUERTA (DEADZONE) =====
        # Si el joystick está casi centrado, considerarlo centrado (evitar drift)
        if abs(velocity_x) < 0.05 and abs(velocity_y) < 0.05:
            if self.is_moving:
                # Acabamos de detenernos: esperar un momento y apagar PWM
                time.sleep(0.15)
                self.stop_pwm()
            return

        # ===== ACTIVAR MODO MOVIMIENTO =====
        self.is_moving = True

        # ===== FACTOR DE VELOCIDAD =====
        # Controla cuántos microsegundos se mueve el servo por cada actualización
        # Ajusta este valor para cambiar la sensibilidad/velocidad de la cámara
        #
        # Actualización cada 100ms (desde Android) → 10 actualizaciones/segundo
        # Con SPEED_FACTOR=25 → 250 µs/segundo a velocidad máxima
        # Rango total: 2000 µs → Tarda ~8 segundos en recorrer todo el rango
        SPEED_FACTOR = 25

        # ===== CALCULAR INCREMENTOS =====
        # El incremento depende de la velocidad del joystick
        delta_x = velocity_x * SPEED_FACTOR          # Incremento horizontal
        delta_y = -velocity_y * SPEED_FACTOR         # Incremento vertical (invertir Y)

        # ===== APLICAR INCREMENTOS (INCREMENTAL, NO ABSOLUTO) =====
        # Esto es lo que hace que sea tipo FPS: sumamos al valor actual
        self.horizontal_pos += int(delta_x)
        self.vertical_pos += int(delta_y)

        # ===== LIMITAR A RANGOS VÁLIDOS =====
        # Evitar que los servos intenten ir más allá de sus límites físicos
        self.horizontal_pos = max(self.MIN_PULSE, min(self.MAX_PULSE, self.horizontal_pos))
        self.vertical_pos = max(self.MIN_PULSE, min(self.MAX_PULSE, self.vertical_pos))

        # ===== APLICAR NUEVA POSICIÓN A LOS SERVOS =====
        self.pwm.setServoPulse(self.SERVO_HORIZONTAL, self.horizontal_pos)
        self.pwm.setServoPulse(self.SERVO_VERTICAL, self.vertical_pos)

    def cleanup(self):
        """
        Limpieza al salir: centra los servos antes de terminar.

        Es buena práctica dejar la cámara centrada al apagar el robot.
        """
        print("🧹 Limpiando controlador de cámara...")
        self.center()


# =============================================================================
# CÓDIGO DE PRUEBA
# =============================================================================
if __name__ == '__main__':
    """
    Código de prueba para verificar el funcionamiento de los servos.
    
    Ejecutar directamente este archivo para probar el movimiento de la cámara:
        python3 camera_control.py
    """
    print("=" * 60)
    print("🧪 MODO DE PRUEBA - CONTROLADOR DE CÁMARA")
    print("=" * 60)

    # Crear controlador con debug activado
    camera = CameraController(debug=True)

    try:
        print("\n📍 Test 1: Centrar cámara")
        camera.center()
        time.sleep(2)

        print("\n➡️  Test 2: Movimiento horizontal (izquierda → derecha)")
        for i in range(-10, 11, 2):
            velocity_x = i / 10.0  # -1.0 a 1.0
            print(f"   Velocidad X: {velocity_x:+.1f}")
            for _ in range(5):  # Simular 5 actualizaciones
                camera.move_incremental(velocity_x, 0)
                time.sleep(0.1)

        print("\n⬆️  Test 3: Movimiento vertical (abajo → arriba)")
        camera.center()
        time.sleep(1)
        for i in range(-10, 11, 2):
            velocity_y = i / 10.0  # -1.0 a 1.0
            print(f"   Velocidad Y: {velocity_y:+.1f}")
            for _ in range(5):  # Simular 5 actualizaciones
                camera.move_incremental(0, velocity_y)
                time.sleep(0.1)

        print("\n🔄 Test 4: Movimiento circular")
        camera.center()
        time.sleep(1)
        for angle in range(0, 360, 15):
            rad = math.radians(angle)
            velocity_x = math.cos(rad) * 0.5
            velocity_y = math.sin(rad) * 0.5
            print(f"   Ángulo: {angle}° → Vel({velocity_x:+.2f}, {velocity_y:+.2f})")
            for _ in range(3):
                camera.move_incremental(velocity_x, velocity_y)
                time.sleep(0.1)

        print("\n✅ Pruebas completadas")

    except KeyboardInterrupt:
        print("\n⛔ Prueba interrumpida por usuario")
    finally:
        camera.cleanup()
        print("👋 Limpieza completada")
