#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
SERVIDOR DE SEGUIMIENTO DE LÍNEA PARA ALPHABOT2
=============================================================================
Este servidor gestiona el seguimiento de línea del robot y recibe comandos
desde la aplicación Android.

AUTOCONTENIDO - No depende de módulos externos, todo el código necesario
está incluido en este archivo.

COMANDOS ACEPTADOS:
- "calibrate"     : Inicia la calibración de los sensores IR
- "start"         : Inicia el seguimiento de línea
- "stop"          : Detiene el seguimiento de línea
- "speed:<valor>" : Ajusta la velocidad máxima (10-100)
- "status"        : Devuelve el estado actual

AUTOR: José Manuel Luque González
FECHA: 2025
=============================================================================
"""

import socket
import threading
import time

try:
    import RPi.GPIO as GPIO
    from rpi_ws281x import Adafruit_NeoPixel, Color
    MOCK_MODE = False
    print("✅ Módulos de hardware disponibles")
except ImportError as e:
    print(f"⚠️ Error importando módulos: {e}")
    print("⚠️ MODO MOCK ACTIVADO - El robot NO se moverá")
    MOCK_MODE = True


# =============================================================================
# CONFIGURACIÓN DE HARDWARE
# =============================================================================
SERVER_HOST = '0.0.0.0'
SERVER_PORT = 5003

# Pines GPIO para motores (AlphaBot2)
AIN1 = 12  # Corregido
AIN2 = 13  # Corregido
PWMA = 6
BIN1 = 20
BIN2 = 21
PWMB = 26

# Pines para sensores IR (TRSensors)
CS = 5
Clock = 25
Address = 24
DataOut = 23
Button = 7

# Configuración de LEDs WS2812
LED_COUNT = 4
LED_PIN = 18
LED_FREQ_HZ = 800000
LED_DMA = 5
LED_BRIGHTNESS = 255
LED_INVERT = False


# =============================================================================
# CLASE: AlphaBot2Control (Control de motores)
# =============================================================================
class AlphaBot2Control:
    """Control de motores del AlphaBot2."""

    def __init__(self):
        """Inicializa los pines GPIO para control de motores."""
        if MOCK_MODE:
            return

        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)

        # Configurar pines de motor A
        GPIO.setup(AIN1, GPIO.OUT)
        GPIO.setup(AIN2, GPIO.OUT)
        GPIO.setup(PWMA, GPIO.OUT)

        # Configurar pines de motor B
        GPIO.setup(BIN1, GPIO.OUT)
        GPIO.setup(BIN2, GPIO.OUT)
        GPIO.setup(PWMB, GPIO.OUT)

        # Configurar PWM
        self.PWMA_ctrl = GPIO.PWM(PWMA, 500)
        self.PWMB_ctrl = GPIO.PWM(PWMB, 500)
        self.PWMA_ctrl.start(0)
        self.PWMB_ctrl.start(0)

    def forward(self):
        """Mover hacia adelante."""
        if MOCK_MODE:
            return
        GPIO.output(AIN1, GPIO.LOW)
        GPIO.output(AIN2, GPIO.HIGH)
        GPIO.output(BIN1, GPIO.LOW)
        GPIO.output(BIN2, GPIO.HIGH)

    def backward(self):
        """Mover hacia atrás."""
        if MOCK_MODE:
            return
        GPIO.output(AIN1, GPIO.HIGH)
        GPIO.output(AIN2, GPIO.LOW)
        GPIO.output(BIN1, GPIO.HIGH)
        GPIO.output(BIN2, GPIO.LOW)

    def left(self):
        """Girar a la izquierda."""
        if MOCK_MODE:
            return
        GPIO.output(AIN1, GPIO.HIGH)
        GPIO.output(AIN2, GPIO.LOW)
        GPIO.output(BIN1, GPIO.LOW)
        GPIO.output(BIN2, GPIO.HIGH)

    def right(self):
        """Girar a la derecha."""
        if MOCK_MODE:
            return
        GPIO.output(AIN1, GPIO.LOW)
        GPIO.output(AIN2, GPIO.HIGH)
        GPIO.output(BIN1, GPIO.HIGH)
        GPIO.output(BIN2, GPIO.LOW)

    def stop(self):
        """Detener motores."""
        if MOCK_MODE:
            return
        GPIO.output(AIN1, GPIO.LOW)
        GPIO.output(AIN2, GPIO.LOW)
        GPIO.output(BIN1, GPIO.LOW)
        GPIO.output(BIN2, GPIO.LOW)

    def setPWMA(self, value):
        """Establecer velocidad del motor A (0-100)."""
        if MOCK_MODE:
            return
        self.PWMA_ctrl.ChangeDutyCycle(value)

    def setPWMB(self, value):
        """Establecer velocidad del motor B (0-100)."""
        if MOCK_MODE:
            return
        self.PWMB_ctrl.ChangeDutyCycle(value)


# =============================================================================
# CLASE: TRSensorControl (Sensores de línea)
# =============================================================================
class TRSensorControl:
    """Control de sensores IR para seguimiento de línea."""

    def __init__(self, numSensors=5):
        """Inicializa los sensores IR."""
        self.numSensors = numSensors
        self.calibratedMin = [0] * self.numSensors
        self.calibratedMax = [1023] * self.numSensors
        self.last_value = 0

        if MOCK_MODE:
            return

        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)
        GPIO.setup(Clock, GPIO.OUT)
        GPIO.setup(Address, GPIO.OUT)
        GPIO.setup(CS, GPIO.OUT)
        GPIO.setup(DataOut, GPIO.IN, GPIO.PUD_UP)
        GPIO.setup(Button, GPIO.IN, GPIO.PUD_UP)

    def AnalogRead(self):
        """Lee los valores analógicos de los sensores."""
        if MOCK_MODE:
            return [500] * self.numSensors

        value = [0] * (self.numSensors + 1)

        # Leer canales 0 a numSensors
        for j in range(0, self.numSensors + 1):
            GPIO.output(CS, GPIO.LOW)

            for i in range(0, 8):
                # Enviar dirección de 8 bits
                if i < 4:
                    if ((j >> (3 - i)) & 0x01):
                        GPIO.output(Address, GPIO.HIGH)
                    else:
                        GPIO.output(Address, GPIO.LOW)
                else:
                    GPIO.output(Address, GPIO.LOW)

                # Leer MSB 4-bit
                value[j] <<= 1
                if GPIO.input(DataOut):
                    value[j] |= 0x01
                GPIO.output(Clock, GPIO.HIGH)
                GPIO.output(Clock, GPIO.LOW)

            for i in range(0, 4):
                # Leer LSB 8-bit
                value[j] <<= 1
                if GPIO.input(DataOut):
                    value[j] |= 0x01
                GPIO.output(Clock, GPIO.HIGH)
                GPIO.output(Clock, GPIO.LOW)

            time.sleep(0.0001)
            GPIO.output(CS, GPIO.HIGH)

        # Desplazar valores
        for i in range(0, 6):
            value[i] >>= 2

        return value[1:]

    def calibrate(self):
        """Calibra los sensores (debe llamarse múltiples veces)."""
        if MOCK_MODE:
            return

        max_sensor_values = [0] * self.numSensors
        min_sensor_values = [0] * self.numSensors

        for j in range(0, 10):
            sensor_values = self.AnalogRead()

            for i in range(0, self.numSensors):
                # Establecer máximo
                if (j == 0) or (max_sensor_values[i] < sensor_values[i]):
                    max_sensor_values[i] = sensor_values[i]

                # Establecer mínimo
                if (j == 0) or (min_sensor_values[i] > sensor_values[i]):
                    min_sensor_values[i] = sensor_values[i]

        # Registrar valores de calibración
        for i in range(0, self.numSensors):
            if min_sensor_values[i] > self.calibratedMin[i]:
                self.calibratedMin[i] = min_sensor_values[i]
            if max_sensor_values[i] < self.calibratedMax[i]:
                self.calibratedMax[i] = max_sensor_values[i]

    def readCalibrated(self):
        """Lee valores calibrados (0-1000)."""
        sensor_values = self.AnalogRead()

        for i in range(0, self.numSensors):
            denominator = self.calibratedMax[i] - self.calibratedMin[i]

            if denominator != 0:
                value = (sensor_values[i] - self.calibratedMin[i]) * 1000 // denominator
            else:
                value = 0

            if value < 0:
                value = 0
            elif value > 1000:
                value = 1000

            sensor_values[i] = value

        return sensor_values

    def readLine(self, white_line=0):
        """Lee la posición de la línea."""
        sensor_values = self.readCalibrated()
        avg = 0
        sum_val = 0
        on_line = 0

        for i in range(0, self.numSensors):
            value = sensor_values[i]
            if white_line:
                value = 1000 - value

            # Verificar si vemos la línea
            if value > 200:
                on_line = 1

            # Promediar solo valores sobre el umbral de ruido
            if value > 50:
                avg += value * (i * 1000)
                sum_val += value

        if on_line != 1:
            # Si última lectura fue a la izquierda del centro
            if self.last_value < (self.numSensors - 1) * 1000 / 2:
                self.last_value = 0
            else:
                # Si última lectura fue a la derecha del centro
                self.last_value = (self.numSensors - 1) * 1000
        else:
            self.last_value = avg // sum_val

        return self.last_value, sensor_values


# =============================================================================
# CLASE: LineFollower (Controlador de seguimiento de línea)
# =============================================================================
class LineFollower:
    """Controlador del algoritmo de seguimiento de línea con PID."""

    def __init__(self):
        """Inicializa el controlador de seguimiento de línea."""
        print("🤖 Inicializando seguimiento de línea...")

        # Estados
        self.is_calibrated = False
        self.is_following = False
        self.speed = 35  # Velocidad máxima por defecto
        self.position = 2000

        # Variables del controlador PID
        self.integral = 0
        self.last_proportional = 0

        # Variables de LEDs
        self.led_j = 0

        # Contador para logging (mostrar cada N iteraciones)
        self.update_counter = 0

        if not MOCK_MODE:
            # Inicializar sensores IR
            self.tr_sensor = TRSensorControl()

            # Inicializar robot
            self.robot = AlphaBot2Control()
            self.robot.stop()

            # Inicializar LEDs
            self.strip = Adafruit_NeoPixel(
                LED_COUNT, LED_PIN, LED_FREQ_HZ,
                LED_DMA, LED_INVERT, LED_BRIGHTNESS
            )
            self.strip.begin()
            self._set_leds_status("ready")

            print("✅ Hardware inicializado")
        else:
            print("⚠️ Modo simulación (sin hardware)")

    def _wheel(self, pos):
        """Genera colores rainbow para los LEDs."""
        if pos < 85:
            return Color(pos * 3, 255 - pos * 3, 0)
        elif pos < 170:
            pos -= 85
            return Color(255 - pos * 3, 0, pos * 3)
        else:
            pos -= 170
            return Color(0, pos * 3, 255 - pos * 3)

    def _set_leds_status(self, status):
        """Configura los LEDs según el estado."""
        if MOCK_MODE:
            return

        if status == "ready":
            for i in range(4):
                self.strip.setPixelColor(i, Color(100, 100, 0))
        elif status == "calibrating":
            for i in range(4):
                self.strip.setPixelColor(i, Color(0, 0, 100))
        elif status == "following":
            for i in range(4):
                self.strip.setPixelColor(i, Color(0, 100, 0))
        elif status == "stopped":
            for i in range(4):
                self.strip.setPixelColor(i, Color(100, 0, 0))

        self.strip.show()

    def calibrate(self):
        """Calibra los sensores de línea moviendo el robot."""
        if MOCK_MODE:
            print("🔧 [MOCK] Calibrando sensores...")
            time.sleep(2)
            self.is_calibrated = True
            return {"status": "success", "message": "Calibración completada (simulada)"}

        print("🔧 Iniciando calibración de sensores...")
        self._set_leds_status("calibrating")

        try:
            # Leer valores iniciales antes de calibrar
            initial_values = self.tr_sensor.AnalogRead()
            print(f"📊 Valores iniciales (sin calibrar): {initial_values}")

            for i in range(100):
                if i < 25 or i >= 75:
                    self.robot.right()
                    self.robot.setPWMA(30)
                    self.robot.setPWMB(30)
                else:
                    self.robot.left()
                    self.robot.setPWMA(30)
                    self.robot.setPWMB(30)

                self.tr_sensor.calibrate()
                time.sleep(0.01)

            self.robot.stop()

            print(f"📊 Min calibrado: {self.tr_sensor.calibratedMin}")
            print(f"📊 Max calibrado: {self.tr_sensor.calibratedMax}")

            # Leer valores después de calibrar para verificar
            time.sleep(0.2)
            calibrated_values = self.tr_sensor.readCalibrated()
            print(f"📊 Valores después de calibrar (0-1000): {calibrated_values}")

            # Verificar que la calibración tiene sentido
            if all(m == 0 for m in self.tr_sensor.calibratedMin) or all(m == 1023 for m in self.tr_sensor.calibratedMax):
                print("⚠️  ADVERTENCIA: Calibración sospechosa - los sensores no detectaron variación")
                print("   Asegúrate de que el robot esté sobre la línea negra")

            self.is_calibrated = True
            self._set_leds_status("ready")

            print("✅ Calibración completada")
            return {"status": "success", "message": "Calibración completada"}

        except Exception as e:
            print(f"❌ Error en calibración: {e}")
            import traceback
            traceback.print_exc()
            self.robot.stop()
            self._set_leds_status("stopped")
            return {"status": "error", "message": str(e)}

    def start(self):
        """Inicia el seguimiento de línea."""
        if not self.is_calibrated:
            return {"status": "error", "message": "Debe calibrar primero"}

        if self.is_following:
            return {"status": "warning", "message": "Ya está siguiendo la línea"}

        print(f"🏃 Iniciando seguimiento de línea (velocidad: {self.speed})...")
        self.is_following = True
        self.integral = 0
        self.last_proportional = 0
        self.update_counter = 0  # Resetear contador de logging

        if not MOCK_MODE:
            self._set_leds_status("following")
            self.robot.forward()

        return {"status": "success", "message": "Seguimiento iniciado"}

    def stop(self):
        """Detiene el seguimiento de línea."""
        if not self.is_following:
            return {"status": "warning", "message": "No estaba siguiendo la línea"}

        print("⏹️ Deteniendo seguimiento de línea...")
        self.is_following = False

        if not MOCK_MODE:
            self.robot.stop()
            self._set_leds_status("stopped")

        return {"status": "success", "message": "Seguimiento detenido"}

    def set_speed(self, new_speed):
        """Ajusta la velocidad máxima del robot."""
        new_speed = max(10, min(100, new_speed))
        self.speed = new_speed
        print(f"⚡ Velocidad ajustada a: {self.speed}")
        return {"status": "success", "message": f"Velocidad: {self.speed}"}

    def update(self):
        """Actualización del bucle principal de seguimiento."""
        if not self.is_following or MOCK_MODE:
            return

        try:
            position, sensors = self.tr_sensor.readLine()
            self.position = position

            # Logging cada 20 iteraciones (~0.2 segundos)
            self.update_counter += 1
            if self.update_counter % 20 == 0:
                print(f"📍 Posición: {position:4.0f} | Sensores: {[f'{s:3.0f}' for s in sensors]}")

            # Línea perdida
            if all(s > 900 for s in sensors):
                self.robot.setPWMA(0)
                self.robot.setPWMB(0)
                if self.update_counter % 20 == 0:
                    print("⚠️  LÍNEA PERDIDA - Todos los sensores en blanco")
                return

            # ========== CONTROL SIMPLE SIN PID (para pruebas) ==========
            # Usar velocidad base fija
            base_speed = self.speed

            # Determinar dirección basándose en la posición
            # Posición: 0 (izquierda) a 4000 (derecha), centro = 2000

            if position < 1500:
                # Línea muy a la izquierda -> Girar FUERTE a la derecha
                motor_a = base_speed
                motor_b = base_speed * 0.3  # Reducir motor derecho
                direction = "→→ DERECHA FUERTE"
            elif position < 1900:
                # Línea ligeramente a la izquierda -> Girar SUAVE a la derecha
                motor_a = base_speed
                motor_b = base_speed * 0.7
                direction = "→ Derecha suave"
            elif position < 2100:
                # Línea centrada -> RECTO
                motor_a = base_speed
                motor_b = base_speed
                direction = "↑ RECTO"
            elif position < 2500:
                # Línea ligeramente a la derecha -> Girar SUAVE a la izquierda
                motor_a = base_speed * 0.7
                motor_b = base_speed
                direction = "← Izquierda suave"
            else:
                # Línea muy a la derecha -> Girar FUERTE a la izquierda
                motor_a = base_speed * 0.3  # Reducir motor izquierdo
                motor_b = base_speed
                direction = "←← IZQUIERDA FUERTE"

            # Aplicar velocidades
            self.robot.setPWMA(motor_a)
            self.robot.setPWMB(motor_b)

            # Logging
            if self.update_counter % 20 == 0:
                print(f"   {direction}: Motor A={motor_a:.0f}, Motor B={motor_b:.0f}")

            # LEDs rainbow
            for i in range(self.strip.numPixels()):
                self.strip.setPixelColor(
                    i,
                    self._wheel((int(i * 256 / self.strip.numPixels()) + self.led_j) & 255)
                )
            self.strip.show()

            self.led_j += 1
            if self.led_j > 256 * 4:
                self.led_j = 0

        except Exception as e:
            print(f"❌ Error en update: {e}")
            self.stop()

    def get_status(self):
        """Devuelve el estado actual del seguidor de línea."""
        return {
            "is_calibrated": self.is_calibrated,
            "is_following": self.is_following,
            "speed": self.speed,
            "position": self.position
        }

    def cleanup(self):
        """Limpieza al cerrar el servidor."""
        print("🧹 Limpiando seguidor de línea...")
        self.is_following = False

        if not MOCK_MODE:
            self.robot.stop()
            for i in range(4):
                self.strip.setPixelColor(i, Color(0, 0, 0))
            self.strip.show()


# =============================================================================
# SERVIDOR TCP
# =============================================================================
class LineFollowServer:
    """Servidor TCP que escucha comandos de la app Android."""

    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.server_socket = None
        self.is_running = False
        self.line_follower = LineFollower()
        self.update_thread = None

    def start(self):
        """Inicia el servidor TCP."""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            self.is_running = True

            print(f"🟢 Servidor de seguimiento de línea iniciado en {self.host}:{self.port}")

            self.update_thread = threading.Thread(target=self._update_loop, daemon=True)
            self.update_thread.start()

            while self.is_running:
                try:
                    client_socket, client_address = self.server_socket.accept()
                    print(f"📱 Cliente conectado: {client_address}")

                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, client_address),
                        daemon=True
                    )
                    client_thread.start()

                except Exception as e:
                    if self.is_running:
                        print(f"❌ Error aceptando conexión: {e}")

        except Exception as e:
            print(f"❌ Error iniciando servidor: {e}")
        finally:
            self.stop()

    def _update_loop(self):
        """Bucle de actualización del seguimiento de línea."""
        print("🔄 Thread de actualización iniciado")

        while self.is_running:
            if self.line_follower.is_following:
                self.line_follower.update()
                time.sleep(0.01)
            else:
                time.sleep(0.1)

    def _handle_client(self, client_socket, client_address):
        """Maneja la comunicación con un cliente."""
        try:
            while self.is_running:
                data = client_socket.recv(1024)

                if not data:
                    break

                command = data.decode('utf-8').strip()
                print(f"📨 Comando recibido: '{command}'")

                response = self._process_command(command)

                response_bytes = f"{response}\n".encode('utf-8')
                client_socket.sendall(response_bytes)
                print(f"📤 Respuesta enviada: '{response}'")

        except Exception as e:
            print(f"❌ Error con cliente {client_address}: {e}")
        finally:
            client_socket.close()
            print(f"👋 Cliente desconectado: {client_address}")

    def _process_command(self, command):
        """Procesa un comando recibido."""
        try:
            command = command.strip().lower()

            if command == "calibrate":
                result = self.line_follower.calibrate()
                return f"OK:{result['message']}" if result['status'] == 'success' else f"ERROR:{result['message']}"

            elif command == "start":
                result = self.line_follower.start()
                return f"OK:{result['message']}" if result['status'] in ['success', 'warning'] else f"ERROR:{result['message']}"

            elif command == "stop":
                result = self.line_follower.stop()
                return f"OK:{result['message']}" if result['status'] in ['success', 'warning'] else f"ERROR:{result['message']}"

            elif command.startswith("speed:"):
                try:
                    speed = int(command.split(":")[1])
                    result = self.line_follower.set_speed(speed)
                    return f"OK:{result['message']}"
                except (IndexError, ValueError) as e:
                    return f"ERROR:Formato de velocidad inválido: {e}"

            elif command == "status":
                status = self.line_follower.get_status()
                return f"OK:{status}"

            else:
                return f"ERROR:Comando desconocido: {command}"

        except Exception as e:
            print(f"❌ Error procesando comando: {e}")
            import traceback
            traceback.print_exc()
            return f"ERROR:{str(e)}"

    def stop(self):
        """Detiene el servidor."""
        print("🛑 Deteniendo servidor...")
        self.is_running = False

        if self.line_follower:
            self.line_follower.cleanup()

        if self.server_socket:
            self.server_socket.close()

        print("👋 Servidor detenido")


# =============================================================================
# PUNTO DE ENTRADA
# =============================================================================
if __name__ == '__main__':
    print("=" * 60)
    print("🤖 SERVIDOR DE SEGUIMIENTO DE LÍNEA - ALPHABOT2")
    print("=" * 60)

    server = LineFollowServer(SERVER_HOST, SERVER_PORT)

    try:
        server.start()
    except KeyboardInterrupt:
        print("\n⛔ Interrupción por usuario")
    finally:
        server.stop()
        print("✅ Servidor cerrado correctamente")

