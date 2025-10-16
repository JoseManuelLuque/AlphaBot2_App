package com.jluqgon214.alphabot2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    dotSize: Dp = 40.dp,
    backgroundColor: Color = Color.LightGray,
    dotColor: Color = Color.DarkGray,
    onMove: (angle: Float, strength: Float) -> Unit
) {
    // El estado del desplazamiento del joystick interior
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* Opcional */ },
                    onDragEnd = {
                        // Resetea el joystick al centro cuando se suelta
                        joystickOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()

                    val newOffset = joystickOffset + dragAmount
                    val radius = size.toPx() / 2

                    // Calculamos la distancia desde el centro
                    val distance = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))

                    // Mantenemos el punto interior dentro del círculo exterior
                    joystickOffset = if (distance <= radius) {
                        newOffset
                    } else {
                        // El punto está fuera, lo normalizamos para que se quede en el borde
                        newOffset.normalized() * radius
                    }

                    // Calculamos el ángulo y la fuerza (strength)
                    val angle = joystickOffset.getAngle()
                    val strength = min(distance / radius, 1.0f) // Normalizamos a un valor entre 0.0 y 1.0

                    onMove(angle, strength)
                }
            }
    ) {
        // Círculo exterior (base)
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(color = backgroundColor, radius = size.toPx() / 2)
        }

        // Círculo interior (pulgar)
        Canvas(modifier = Modifier.size(dotSize)) {
            // Aplicamos el desplazamiento calculado
            translate(left = joystickOffset.x, top = joystickOffset.y) {
                drawCircle(color = dotColor, radius = dotSize.toPx() / 2)
            }
        }
    }
}

// Funciones de utilidad para el cálculo
private fun Offset.normalized(): Offset {
    val magnitude = sqrt(x * x + y * y)
    return if (magnitude != 0f) this / magnitude else Offset.Zero
}

private fun Offset.getAngle(): Float {
    // atan2 devuelve el ángulo en radianes. Lo convertimos a grados.
    // El eje Y está invertido en Compose, por eso usamos -y.
    return (atan2(-y, x) * (180f / Math.PI)).toFloat()
}

// Extensión para la potencia, más legible que Math.pow
private fun Float.pow(n: Int): Float = this.pow(n)