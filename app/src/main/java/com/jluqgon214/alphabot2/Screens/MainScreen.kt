package com.jluqgon214.alphabot2.Screens

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.viewinterop.AndroidView
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

    // Variables para joystick de movimiento
    var joystickX by remember { mutableStateOf(0f) }
    var joystickY by remember { mutableStateOf(0f) }

    // Variables para joystick de cámara
    var cameraX by remember { mutableStateOf(0f) }
    var cameraY by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    // Radio del joystick para normalizar (150dp / 2 = 75)
    val joystickRadius = 75f

    // URL del stream de video
    val streamUrl = "http://$host:8080/stream.mjpg"

    LaunchedEffect(Unit) {
        // Conectar por SSH para iniciar el servidor
        SSHManager.connect(host, user, password) { sshConnected ->
            if (sshConnected) {
                statusText = "SSH conectado. Iniciando servidores..."

                // Matar cualquier servidor anterior
                SSHManager.executeCommand("pkill -f joystick_server.py; pkill -f camera_stream.py; pkill -f start_servers.py") { _ ->
                    coroutineScope.launch {
                        delay(500)

                        // Usar el script de inicio unificado que es más robusto
                        SSHManager.executeCommand("python3 '/home/pi/Android App/start_servers.py'") { result ->
                            Log.d("ServerStart", "Resultado inicio: $result")
                            statusText = "Servidores iniciados. Conectando..."

                            coroutineScope.launch {
                                delay(3000)  // Dar más tiempo para que los servidores se inicien

                                // Conectar por socket al servidor de control
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

                // ========== CONTROL DE MOVIMIENTO (MOTORES) ==========
                // Normalizar valores del joystick de movimiento
                val normalizedX = (-joystickX / joystickRadius).coerceIn(-1f, 1f)
                val normalizedY = (joystickY / joystickRadius).coerceIn(-1f, 1f)

                // Calcular magnitud para movimiento
                val magnitude =
                    sqrt(normalizedX * normalizedX + normalizedY * normalizedY).coerceIn(0f, 1f)

                // Zona muerta del 20% para movimiento
                if (magnitude > 0.2f) {
                    SocketManager.sendJoystickData(normalizedX, normalizedY)
                } else {
                    // Detener cuando está en zona muerta
                    SocketManager.sendJoystickData(0f, 0f)
                }

                // ========== CONTROL DE CÁMARA (TIPO FPS - VELOCIDAD INCREMENTAL) ==========
                // La cámara usa la posición del joystick como VELOCIDAD de rotación
                // Si mantienes el joystick a la derecha, la cámara SIGUE girando a la derecha

                val rawCameraX = (cameraX / joystickRadius).coerceIn(-1f, 1f)
                val rawCameraY = (cameraY / joystickRadius).coerceIn(-1f, 1f)

                // Calcular magnitud
                val cameraMagnitude = sqrt(rawCameraX * rawCameraX + rawCameraY * rawCameraY)

                // Zona muerta del 15%
                if (cameraMagnitude > 0.15f) {
                    // El joystick indica VELOCIDAD de movimiento
                    // Valores pequeños = rotación lenta
                    // Valores grandes = rotación rápida
                    SocketManager.sendCameraData(rawCameraX, rawCameraY)
                    Log.d("CameraControl", "Velocidad cámara: x=$rawCameraX, y=$rawCameraY")
                } else {
                    // Si está centrado, o se suelta el joystick, NO mover la cámara (velocidad = 0)
                    // La cámara se queda donde está
                    SocketManager.sendCameraData(0f, 0f)
                }
            }
        }
    }

    // Limpieza al salir de la pantalla
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
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Estado de conexión
            Text(
                text = statusText,
                modifier = Modifier.padding(16.dp)
            )

            // Stream de video (solo se muestra si está conectado)
            if (isConnected) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            // Configuración de WebView
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false

                            // Eliminar bordes y fondos blancos
                            setBackgroundColor(0x00000000) // Transparente
                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                            // Mejor manejo de errores
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    Log.e("StreamError", "Error cargando stream: $description")
                                    Log.e("StreamError", "URL: $failingUrl")
                                    Log.e("StreamError", "Código: $errorCode")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("StreamSuccess", "Stream cargado: $url")
                                }
                            }

                            Log.d("StreamURL", "Intentando cargar: $streamUrl")
                            // Cargar el stream MJPEG
                            loadUrl(streamUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 16.dp),
                    update = { webView ->
                        // Recargar si es necesario
                        if (webView.url != streamUrl) {
                            Log.d("StreamURL", "Recargando stream: $streamUrl")
                            webView.loadUrl(streamUrl)
                        }
                    }
                )
            }

            // Joysticks de control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Joystick de control de movimiento
                JoyStick(
                    Modifier.padding(16.dp),
                    size = 150.dp,
                    dotSize = 40.dp,
                    backgroundImage = R.drawable.base,
                    dotImage = R.drawable.top,
                ) { x: Float, y: Float ->
                    joystickX = x
                    joystickY = -y
                }

                // Joystick de control de cámara
                JoyStick(
                    Modifier.padding(16.dp),
                    size = 150.dp,
                    dotSize = 40.dp,
                    backgroundImage = R.drawable.base,
                    dotImage = R.drawable.top,
                ) { x: Float, y: Float ->
                    cameraX = -x
                    cameraY = y
                }
            }
        }
    }
}