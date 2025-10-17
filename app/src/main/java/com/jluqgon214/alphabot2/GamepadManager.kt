package com.jluqgon214.alphabot2

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ComponentActivity

/**
 * =============================================================================
 * GESTOR DE MANDOS PARA ALPHABOT2
 * =============================================================================
 * Maneja la entrada de mandos de videojuegos (PS5 DualSense, Xbox, etc.)
 * conectados por Bluetooth al dispositivo Android.
 *
 * CONTROLES MAPEADOS:
 * - Stick Izquierdo: Control de movimiento del robot
 * - Stick Derecho: Control de cámara (tipo FPS)
 * - Botones L2/R2: Velocidad turbo
 * - Botón Circle/B: Centrar cámara
 * - Botón Options: Desconectar
 *
 * AUTOR: José Manuel Luque González
 * FECHA: 2025
 * =============================================================================
 */

/**
 * Estado del gamepad con todos los valores de entrada
 */
data class GamepadState(
    val isConnected: Boolean = false,
    val deviceName: String = "",

    // Stick izquierdo (movimiento)
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,

    // Stick derecho (cámara)
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    // Triggers
    val leftTrigger: Float = 0f,   // L2
    val rightTrigger: Float = 0f,  // R2

    // Botones
    val buttonA: Boolean = false,      // Cruz (PS) / A (Xbox)
    val buttonB: Boolean = false,      // Círculo (PS) / B (Xbox)
    val buttonX: Boolean = false,      // Cuadrado (PS) / X (Xbox)
    val buttonY: Boolean = false,      // Triángulo (PS) / Y (Xbox)
    val buttonL1: Boolean = false,
    val buttonR1: Boolean = false,
    val buttonStart: Boolean = false,  // Options (PS) / Menu (Xbox)
    val buttonSelect: Boolean = false, // Share (PS) / View (Xbox)
)

/**
 * Clase para gestionar la entrada del gamepad
 */
class GamepadManager {
    // Cambiar a mutableStateOf para que sea observable
    var state by mutableStateOf(GamepadState())
        private set

    // Zona muerta para los sticks (evitar drift)
    private val deadzone = 0.15f

    /**
     * Detecta gamepads conectados
     */
    fun detectGamepads(): List<InputDevice> {
        val gameControllerDeviceIds = mutableListOf<Int>()
        val deviceIds = InputDevice.getDeviceIds()

        deviceIds.forEach { deviceId ->
            InputDevice.getDevice(deviceId)?.apply {
                val sources = this.sources

                // Verificar si es un gamepad
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                    gameControllerDeviceIds.add(deviceId)
                    Log.d("GamepadManager", "Gamepad encontrado: ${this.name} (ID: $deviceId)")
                }
            }
        }

        return gameControllerDeviceIds.mapNotNull { InputDevice.getDevice(it) }
    }

    /**
     * Actualiza el estado de conexión
     */
    fun updateConnectionState() {
        val gamepads = detectGamepads()
        val wasConnected = state.isConnected
        val isNowConnected = gamepads.isNotEmpty()

        state = state.copy(
            isConnected = isNowConnected,
            deviceName = gamepads.firstOrNull()?.name ?: ""
        )

        if (isNowConnected && !wasConnected) {
            Log.d("GamepadManager", "✅ Mando conectado: ${gamepads.first().name}")
        } else if (!isNowConnected && wasConnected) {
            Log.d("GamepadManager", "❌ Mando desconectado")
        }
    }

    /**
     * Aplica zona muerta a un valor del stick
     */
    private fun applyDeadzone(value: Float): Float {
        return if (kotlin.math.abs(value) < deadzone) 0f else value
    }

    /**
     * Procesa eventos de movimiento (sticks y triggers)
     */
    fun onMotionEvent(event: MotionEvent): Boolean {
        // Verificar que sea de un gamepad
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK &&
            event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) {
            return false
        }

        // Actualizar estado de conexión al recibir evento
        if (!state.isConnected) {
            updateConnectionState()
        }

        // Leer valores de los sticks
        val leftX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
        val leftY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
        val rightX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z))
        val rightY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ))

        // Leer triggers (L2/R2)
        val leftTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rightTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

        // Actualizar estado de forma observable
        state = state.copy(
            leftStickX = leftX,
            leftStickY = leftY,
            rightStickX = rightX,
            rightStickY = rightY,
            leftTrigger = leftTrigger,
            rightTrigger = rightTrigger
        )

        // Log para debug (solo cuando hay movimiento significativo)
        if (leftX != 0f || leftY != 0f || rightX != 0f || rightY != 0f) {
            Log.d("GamepadInput", "Sticks - L(${String.format("%.2f", leftX)}, ${String.format("%.2f", leftY)}) R(${String.format("%.2f", rightX)}, ${String.format("%.2f", rightY)})")
        }

        return true
    }

    /**
     * Procesa eventos de botones
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        // Actualizar estado de conexión al recibir evento
        if (!state.isConnected) {
            updateConnectionState()
        }

        val pressed = event.action == KeyEvent.ACTION_DOWN

        Log.d("GamepadButton", "Botón: ${event.keyCode} - ${if (pressed) "PRESIONADO" else "SOLTADO"}")

        when (event.keyCode) {
            // Botones principales
            KeyEvent.KEYCODE_BUTTON_A -> {
                state = state.copy(buttonA = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                state = state.copy(buttonB = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_X -> {
                state = state.copy(buttonX = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> {
                state = state.copy(buttonY = pressed)
                return true
            }

            // Bumpers
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                state = state.copy(buttonL1 = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                state = state.copy(buttonR1 = pressed)
                return true
            }

            // Start/Options y Select/Share
            KeyEvent.KEYCODE_BUTTON_START -> {
                state = state.copy(buttonStart = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                state = state.copy(buttonSelect = pressed)
                return true
            }
        }

        return false
    }

    /**
     * Resetea el estado del gamepad
     */
    fun reset() {
        state = GamepadState()
    }
}

/**
 * Composable para usar el GamepadManager en la UI
 */
@Composable
fun rememberGamepadManager(): GamepadManager {
    val context = LocalContext.current
    val gamepadManager = remember { GamepadManager() }

    DisposableEffect(Unit) {
        // Detectar gamepads al iniciar
        gamepadManager.updateConnectionState()

        // Configurar callbacks en la Activity
        val activity = context as? ComponentActivity
        activity?.let { act ->
            val originalOnGenericMotionEvent = act::onGenericMotionEvent
            val originalOnKeyDown = act::onKeyDown
            val originalOnKeyUp = act::onKeyUp

            // No podemos sobrescribir directamente, pero el sistema de Android
            // ya distribuye estos eventos correctamente
        }

        onDispose {
            gamepadManager.reset()
        }
    }

    return gamepadManager
}
