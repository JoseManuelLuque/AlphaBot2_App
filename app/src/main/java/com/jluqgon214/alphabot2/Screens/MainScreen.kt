package com.jluqgon214.alphabot2.Screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jluqgon214.alphabot2.R
import com.jluqgon214.alphabot2.SSHManager
import com.jluqgon214.alphabot2.SocketManager
import com.manalkaff.jetstick.JoyStick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun MainScreen(host: String, user: String, password: String, innerPadding: PaddingValues) {
    var statusText by remember { mutableStateOf("Iniciando...") }
    var isConnected by remember { mutableStateOf(false) }
    var joystickX by remember { mutableStateOf(0f) }
    var joystickY by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Radio del joystick para normalizar (150dp / 2 = 75)
    val joystickRadius = 75f

    LaunchedEffect(Unit) {
        // Conectar por SSH para iniciar el servidor
        SSHManager.connect(host, user, password) { sshConnected ->
            if (sshConnected) {
                statusText = "SSH conectado. Iniciando servidor..."

                // Matar cualquier servidor anterior
                SSHManager.executeCommand("pkill -f joystick_server.py") { _ ->
                    coroutineScope.launch {
                        delay(500)

                        // Iniciar el servidor en la Raspberry Pi
                        SSHManager.executeCommand("nohup python3 '/home/pi/Android App/joystick_server.py' > /tmp/joystick_server.log 2>&1 &") { _ ->
                            statusText = "Servidor iniciado. Conectando..."

                            coroutineScope.launch {
                                delay(1500)

                                // Conectar por socket
                                SocketManager.connect(host) { socketConnected ->
                                    isConnected = socketConnected
                                    statusText = if (socketConnected) {
                                        "✅ Conectado - Control activo"
                                    } else {
                                        "❌ Error conectando al servidor"
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                statusText = "❌ Error en la conexión SSH"
            }
        }
    }

    // Envío continuo de comandos por socket
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (SocketManager.isConnected()) {
                delay(100) // 10 comandos por segundo

                // Normalizar valores del joystick
                val normalizedX = (-joystickX / joystickRadius).coerceIn(-1f, 1f)
                val normalizedY = (joystickY / joystickRadius).coerceIn(-1f, 1f)

                // Calcular magnitud
                val magnitude = sqrt(normalizedX * normalizedX + normalizedY * normalizedY).coerceIn(0f, 1f)

                // Zona muerta del 20%
                if (magnitude > 0.2f) {
                    SocketManager.sendJoystickData(normalizedX, normalizedY)
                    Log.d("JoystickControl", "x=$normalizedX, y=$normalizedY, mag=$magnitude")
                } else {
                    // Detener cuando está en zona muerta
                    SocketManager.sendJoystickData(0f, 0f)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Limpiar al salir
            coroutineScope.launch {
                SocketManager.stop()
                delay(100)
                SocketManager.disconnect()
                SSHManager.disconnect()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(text = statusText)

            JoyStick(
                Modifier.padding(30.dp),
                size = 200.dp,
                dotSize = 50.dp,
                backgroundImage = R.drawable.base,
                dotImage = R.drawable.top,
            ) { x: Float, y: Float ->
                joystickX = x
                joystickY = -y // Invertir Y porque la dirección de giro estaba invertida en un principio
            }
        }
    }
}