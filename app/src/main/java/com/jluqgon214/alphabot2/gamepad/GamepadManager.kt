package com.jluqgon214.alphabot2.gamepad

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Gestor de mandos para AlphaBot2
 */
class GamepadManager {
    var state by mutableStateOf(GamepadState())
        private set

    private val deadzone = 0.15f

    fun detectGamepads(): List<InputDevice> {
        val gameControllerDeviceIds = mutableListOf<Int>()
        val deviceIds = InputDevice.getDeviceIds()

        deviceIds.forEach { deviceId ->
            InputDevice.getDevice(deviceId)?.apply {
                val sources = this.sources

                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                    gameControllerDeviceIds.add(deviceId)
                    Log.d("GamepadManager", "Gamepad encontrado: ${this.name} (ID: $deviceId)")
                }
            }
        }

        return gameControllerDeviceIds.mapNotNull { InputDevice.getDevice(it) }
    }

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

    private fun applyDeadzone(value: Float): Float {
        return if (kotlin.math.abs(value) < deadzone) 0f else value
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK &&
            event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) {
            return false
        }

        if (!state.isConnected) {
            updateConnectionState()
        }

        val leftX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
        val leftY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
        val rightX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z))
        val rightY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ))

        val leftTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rightTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

        state = state.copy(
            leftStickX = leftX,
            leftStickY = leftY,
            rightStickX = rightX,
            rightStickY = rightY,
            leftTrigger = leftTrigger,
            rightTrigger = rightTrigger
        )

        return true
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!state.isConnected) {
            updateConnectionState()
        }

        val pressed = event.action == KeyEvent.ACTION_DOWN

        when (event.keyCode) {
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
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                state = state.copy(buttonL1 = pressed)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                state = state.copy(buttonR1 = pressed)
                return true
            }
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
}
