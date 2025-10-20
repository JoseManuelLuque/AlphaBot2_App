package com.jluqgon214.alphabot2.gamepad

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
    val buttonA: Boolean = false,
    val buttonB: Boolean = false,
    val buttonX: Boolean = false,
    val buttonY: Boolean = false,
    val buttonL1: Boolean = false,
    val buttonR1: Boolean = false,
    val buttonStart: Boolean = false,
    val buttonSelect: Boolean = false,
)

