package com.jluqgon214.alphabot2.screens

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jluqgon214.alphabot2.R
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.models.SpeedMode
import com.jluqgon214.alphabot2.network.SSHManager
import com.jluqgon214.alphabot2.network.SocketManager
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
    var cameraAvailable by remember { mutableStateOf(true) } // Asumir cámara disponible por defecto

    // Variables para joystick táctil de movimiento
    var joystickX by remember { mutableStateOf(0f) }
    var joystickY by remember { mutableStateOf(0f) }

    // Variables para joystick táctil de cámara
    var cameraX by remember { mutableStateOf(0f) }
    var cameraY by remember { mutableStateOf(0f) }

    // ===== MODO DE VELOCIDAD PARA CONTROL TÁCTIL =====
    var speedMode by remember { mutableStateOf(SpeedMode.STANDARD) }

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
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        // Conectar por SSH para iniciar el servidor
        SSHManager.connect(host, user, password) { sshConnected ->
            if (sshConnected) {
                statusText = "SSH conectado. Iniciando servidores..."

                // start_servers.py ya se encarga de matar procesos antiguos
                // Usar sudo porque los scripts necesitan acceso a GPIO
                SSHManager.executeCommand("sudo python3 '/home/pi/Android App/start_servers.py' 2>&1") { result ->
                    Log.d("ServerStart", "Resultado inicio: $result")

                    // Verificar estado de los servidores
                    val cameraInactive = result.contains("❌ INACTIVO") || result.contains("Cámara no detectada")
                    val controlActive = result.contains("Servidor de control:  ✅ ACTIVO")

                    if (cameraInactive) {
                        Log.w("ServerStart", "Cámara no disponible, pero continuando con control")
                        cameraAvailable = false
                    }

                    if (controlActive) {
                        statusText = "Servidor de control iniciado. Conectando..."

                        // INTENTAR CONECTAR AL SOCKET SIEMPRE, aunque la cámara falle
                        coroutineScope.launch {
                            delay(2000) // Dar tiempo al servidor

                            SocketManager.connect(host) { socketConnected ->
                                isConnected = socketConnected
                                statusText = if (socketConnected) {
                                    if (cameraAvailable) {
                                        "✅ Conectado - Control activo"
                                    } else {
                                        "✅ Control activo (Cámara no disponible)"
                                    }
                                } else {
                                    "❌ Error conectando al servidor de control"
                                }
                            }
                        }
                    } else {
                        // Solo falla si NO hay servidor de control
                        statusText = "❌ Servidor de control no inició"
                        Log.e("ServerStart", "Servidor de control no disponible")

                        // Leer logs para diagnóstico
                        coroutineScope.launch {
                            delay(500)
                            SSHManager.executeCommand("echo '=== LOG JOYSTICK SERVER ===' && cat /tmp/joystick_server.log 2>&1 && echo '\n=== LOG CAMERA STREAM ===' && cat /tmp/camera_stream.log 2>&1") { logs ->
                                Log.e("ServerLogs", "Logs de error:\n$logs")
                            }
                        }
                    }
                }
            } else {
                statusText = "❌ Error en la conexión SSH"
            }
        }
    }

    // Envío continuo de comandos por socket (CON SISTEMA DE VELOCIDAD)
    LaunchedEffect(isConnected, speedMode) {
        if (isConnected) {
            Log.d("MainScreen", "Bucle de control iniciado")

            while (SocketManager.isConnected()) {
                delay(50)

                val currentGamepadState = gamepadManager.state

                // ========== DETERMINAR FUENTE DE INPUT Y VELOCIDAD ==========
                val (moveX, moveY, camX, camY, speedMultiplier) = if (currentGamepadState.isConnected) {
                    // ===== CONTROL POR GAMEPAD =====
                    val gMoveX = -currentGamepadState.leftStickX
                    val gMoveY = currentGamepadState.leftStickY
                    val gCamX = -currentGamepadState.rightStickX
                    val gCamY = -currentGamepadState.rightStickY

                    // Calcular velocidad basada en gatillos L2/R2 (ANALÓGICO)
                    val l2 = currentGamepadState.leftTrigger
                    val r2 = currentGamepadState.rightTrigger
                    val speedMult = SpeedMode.fromTriggers(l2, r2)

                    listOf(gMoveX, gMoveY, gCamX, gCamY, speedMult)
                } else {
                    // ===== CONTROL POR JOYSTICKS TÁCTILES =====
                    val tMoveX = (-joystickX / joystickRadius).coerceIn(-1f, 1f)
                    val tMoveY = (joystickY / joystickRadius).coerceIn(-1f, 1f)
                    val tCamX = (cameraX / joystickRadius).coerceIn(-1f, 1f)
                    val tCamY = (cameraY / joystickRadius).coerceIn(-1f, 1f)

                    // Usar el modo de velocidad seleccionado en los botones
                    val speedMult = speedMode.multiplier

                    listOf(tMoveX, tMoveY, tCamX, tCamY, speedMult)
                }

                // ========== CONTROL DE MOVIMIENTO CON LÍMITE DE VELOCIDAD ==========
                val moveMagnitude = sqrt(moveX * moveX + moveY * moveY).coerceIn(0f, 1f)

                if (moveMagnitude > 0.2f) {
                    // APLICAR MULTIPLICADOR DE VELOCIDAD
                    val adjustedMoveX = moveX * speedMultiplier
                    val adjustedMoveY = moveY * speedMultiplier

                    // Log eliminado para mejorar rendimiento (se ejecutaba cada ~50ms)
                    SocketManager.sendJoystickData(adjustedMoveX, adjustedMoveY)
                } else {
                    SocketManager.sendJoystickData(0f, 0f)
                }

                // ========== CONTROL DE CÁMARA ==========
                val cameraMagnitude = sqrt(camX * camX + camY * camY)

                if (cameraMagnitude > 0.15f) {
                    SocketManager.sendCameraData(camX, camY)
                } else {
                    SocketManager.sendCameraData(0f, 0f)
                }
            }
        }
    }

    // Limpieza al salir
    DisposableEffect(Unit) {
        onDispose {
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
            // ========== ESTADO DE CONEXIÓN ==========
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = statusText)

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

            // ========== STREAM DE VIDEO ==========
            // Intentar mostrar cámara solo si está disponible
            if (cameraAvailable) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            settings.mediaPlaybackRequiresUserGesture = false
                            setBackgroundColor(0xFF1E1E1E.toInt())
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    Log.e("StreamError", "Error cargando stream - Codigo: $errorCode, Desc: $description")
                                    // No desactivar la cámara, puede ser temporal
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("StreamSuccess", "Stream cargado correctamente")
                                }
                            }

                            Log.d("StreamInit", "Iniciando carga de stream desde: $streamUrl")
                            loadUrl(streamUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 16.dp),
                    update = { webView ->
                        if (webView.url != streamUrl) {
                            Log.d("StreamUpdate", "Actualizando URL del stream a: $streamUrl")
                            webView.loadUrl(streamUrl)
                        }
                    }
                )
            } else {
                // Mostrar mensaje si la cámara no está disponible
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📷 Cámara no disponible", fontSize = 18.sp)
                            Text("Los controles siguen funcionando", fontSize = 14.sp)
                            Button(
                                onClick = { cameraAvailable = true }
                            ) {
                                Text("🔄 Reintentar")
                            }
                        }
                    }
                }
            }

            // ========== CONTROLES ==========
            // Los controles SIEMPRE están disponibles, conectado o no
            if (!gamepadState.isConnected) {
                // MODO TÁCTIL: Joysticks + Botones de velocidad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botones de velocidad
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Botón Lento
                        FilterChip(
                            selected = speedMode == SpeedMode.SLOW,
                            onClick = { speedMode = SpeedMode.SLOW },
                            label = { Text("${SpeedMode.SLOW.icon} ${SpeedMode.SLOW.displayName}") },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            enabled = isConnected // Solo habilitado si hay conexión
                        )

                        // Botón Estándar
                        FilterChip(
                            selected = speedMode == SpeedMode.STANDARD,
                            onClick = { speedMode = SpeedMode.STANDARD },
                            label = { Text("${SpeedMode.STANDARD.icon} ${SpeedMode.STANDARD.displayName}") },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            enabled = isConnected // Solo habilitado si hay conexión
                        )

                        // Botón Rápido
                        FilterChip(
                            selected = speedMode == SpeedMode.FAST,
                            onClick = { speedMode = SpeedMode.FAST },
                            label = { Text("${SpeedMode.FAST.icon} ${SpeedMode.FAST.displayName}") },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            enabled = isConnected // Solo habilitado si hay conexión
                        )
                    }

                    // Joysticks - SIEMPRE VISIBLES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Joystick de movimiento
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                            Text("🤖 Robot", fontSize = 12.sp)
                        }

                        // Joystick de cámara
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                            Text("📹 Cámara", fontSize = 12.sp)
                        }
                    }

                    // Mensaje si no hay conexión
                    if (!isConnected) {
                        Text(
                            text = "⚠️ Sin conexión - Esperando servidor...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // MODO GAMEPAD: Indicadores
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Controles Gamepad:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🕹️ Stick Izq: Movimiento | Stick Der: Cámara")
                    Text("⚡ L2: Reducir velocidad | R2: Aumentar velocidad")
                    Text("🔘 B/Circle: Centrar cámara")
                }
            }
        }
    }
}
