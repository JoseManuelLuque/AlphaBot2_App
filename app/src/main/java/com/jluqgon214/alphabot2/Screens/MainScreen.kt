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
import com.jluqgon214.alphabot2.GamepadManager
import com.jluqgon214.alphabot2.R
import com.jluqgon214.alphabot2.SSHManager
import com.jluqgon214.alphabot2.SocketManager
import com.manalkaff.jetstick.JoyStick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun MainScreen(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues,
    gamepadManager: GamepadManager
) {
    var statusText by remember { mutableStateOf("Iniciando...") }
    var isConnected by remember { mutableStateOf(false) }

    // Variables para joystick táctil de movimiento
    var joystickX by remember { mutableStateOf(0f) }
    var joystickY by remember { mutableStateOf(0f) }

    // Variables para joystick táctil de cámara
    var cameraX by remember { mutableStateOf(0f) }
    var cameraY by remember { mutableStateOf(0f) }

    // Estado del gamepad
    val gamepadState = gamepadManager.state

    val coroutineScope = rememberCoroutineScope()

    // Radio del joystick para normalizar (150dp / 2 = 75)
    val joystickRadius = 75f

    // URL del stream de video
    val streamUrl = "http://$host:8080/stream.mjpg"

    // Detectar gamepads periódicamente
    LaunchedEffect(Unit) {
        while (true) {
            gamepadManager.updateConnectionState()
            delay(2000) // Verificar cada 2 segundos
        }
    }

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

    // Envío continuo de comandos por socket (actualizado para gamepad)
    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.d("MainScreen", "Bucle de control iniciado")

            while (SocketManager.isConnected()) {
                delay(50) // 20 comandos por segundo para movimiento más suave

                // Leer el estado actual del gamepad directamente
                val currentGamepadState = gamepadManager.state

                // ========== DETERMINAR FUENTE DE INPUT ==========
                // Prioridad: Gamepad > Joysticks táctiles

                val (moveX, moveY, camX, camY) = if (currentGamepadState.isConnected) {
                    // ===== CONTROL POR GAMEPAD =====
                    // Stick izquierdo para movimiento (invertir ambos ejes)
                    val gMoveX = -currentGamepadState.leftStickX
                    val gMoveY = currentGamepadState.leftStickY

                    // Stick derecho para cámara (invertir solo X)
                    val gCamX = -currentGamepadState.rightStickX
                    val gCamY = -currentGamepadState.rightStickY // Invertir Y

                    listOf(gMoveX, gMoveY, gCamX, gCamY)
                } else {
                    // ===== CONTROL POR JOYSTICKS TÁCTILES =====
                    val tMoveX = (-joystickX / joystickRadius).coerceIn(-1f, 1f)
                    val tMoveY = (joystickY / joystickRadius).coerceIn(-1f, 1f)
                    val tCamX = (cameraX / joystickRadius).coerceIn(-1f, 1f)
                    val tCamY = (cameraY / joystickRadius).coerceIn(-1f, 1f)

                    listOf(tMoveX, tMoveY, tCamX, tCamY)
                }

                // ========== CONTROL DE MOVIMIENTO (MOTORES) ==========
                val moveMagnitude = sqrt(moveX * moveX + moveY * moveY).coerceIn(0f, 1f)

                // Zona muerta del 20% para movimiento
                if (moveMagnitude > 0.2f) {
                    if (currentGamepadState.isConnected) {
                        Log.d("GamepadControl", "Movimiento: x=${String.format("%.2f", moveX)}, y=${String.format("%.2f", moveY)}")
                    }
                    SocketManager.sendJoystickData(moveX, moveY)
                } else {
                    SocketManager.sendJoystickData(0f, 0f)
                }

                // ========== CONTROL DE CÁMARA (TIPO FPS - VELOCIDAD INCREMENTAL) ==========
                val cameraMagnitude = sqrt(camX * camX + camY * camY)

                // Zona muerta del 15%
                if (cameraMagnitude > 0.15f) {
                    SocketManager.sendCameraData(camX, camY)
                    if (currentGamepadState.isConnected) {
                        Log.d("GamepadControl", "Cámara: x=${String.format("%.2f", camX)}, y=${String.format("%.2f", camY)}")
                    }
                } else {
                    SocketManager.sendCameraData(0f, 0f)
                }

                // ========== BOTONES DEL GAMEPAD ==========
                if (currentGamepadState.isConnected) {
                    // Botón B/Circle: Centrar cámara (solo al presionar, no mantener)
                    // TODO: Implementar comando de centrado de cámara

                    // Botón Start/Options: Desconectar (puedes implementar si quieres)
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
            // Estado de conexión (actualizado para mostrar gamepad)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = statusText)

                // Mostrar estado del gamepad
                if (gamepadState.isConnected) {
                    Text(
                        text = "🎮 ${gamepadState.deviceName}",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "🎮 Mando no detectado - Usando táctil",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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

            // Joysticks de control táctil (se ocultan si hay gamepad conectado)
            if (!gamepadState.isConnected) {
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
            } else {
                // Mostrar indicadores del gamepad cuando está conectado
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Controles Gamepad:")
                    Text("🕹️ Stick Izq: Movimiento | Stick Der: Cámara")
                    Text("🔘 Circle/B: Centrar cámara | L2/R2: Turbo")
                }
            }
        }
    }
}