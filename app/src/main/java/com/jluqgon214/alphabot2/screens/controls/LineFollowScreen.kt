package com.jluqgon214.alphabot2.screens.controls

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jluqgon214.alphabot2.network.LineFollowManager
import com.jluqgon214.alphabot2.network.SSHManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de control de seguimiento de línea (Line Follow).
 *
 * Permite al robot seguir automáticamente una línea usando sensores infrarrojo.
 *
 * Funcionalidades:
 * - Calibrar sensores
 * - Ajustar velocidad de seguimiento
 * - Iniciar/detener seguimiento
 *
 * Nota: Se conecta vía SSH al robot.
 * Esta es una pantalla de interfaz gráfica; la lógica principal está en LineFollowManager.
 *
 * @param host IP del robot
 * @param user Usuario SSH del robot
 * @param password Contraseña SSH del robot
 * @param innerPadding PaddingValues para respetar barras del sistema
 */

@Composable
fun LineFollowScreen(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues
) {
    var statusText by remember { mutableStateOf("Conectando al servidor...") }
    var isConnected by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(35f) }
    var isCalibrating by remember { mutableStateOf(false) }
    var isCalibrated by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Conectar al servidor al iniciar
    LaunchedEffect(Unit) {
        // Primero conectar por SSH para verificar que el servidor está iniciado
        SSHManager.connect(host, user, password) { sshConnected ->
            if (sshConnected) {
                statusText = "SSH conectado. Verificando servidor..."

                // Conectar al servidor de seguimiento de línea
                coroutineScope.launch {
                    delay(1000) // Dar tiempo al servidor

                    val connected = LineFollowManager.connect(host)
                    isConnected = connected

                    statusText = if (connected) {
                        "✅ Conectado - Listo para calibrar"
                    } else {
                        "❌ Error: Servidor de seguimiento no disponible"
                    }
                }
            } else {
                statusText = "❌ Error en la conexión SSH"
            }
        }
    }

    // Limpieza al salir
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                if (isFollowing) {
                    LineFollowManager.stop()
                }
                LineFollowManager.disconnect()

                // Me gusta cerrar las conexiones SSH para que no se vayan acumulando, aunque el servidor
                // se encargue de cerrarlas al cerrar la sesión, prefiero hacerlo yo mismo para evitar problemas
                SSHManager.disconnect()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título y estado
        Text(
            text = "Seguimiento de Línea",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp
            )
        }

        // Estado del seguimiento
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFollowing)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFollowing) "🏃 ROBOT EN MOVIMIENTO" else "⏸️ ROBOT DETENIDO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isFollowing) {
                    Text(
                        text = "Siguiendo la línea...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Velocidad: ${currentSpeed.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isCalibrating) {
                    Text(
                        text = "⚙️ Calibrando sensores...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "El robot se moverá automáticamente",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Coloca el robot sobre la línea",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "y presiona CALIBRAR primero",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Panel de control
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Control de velocidad
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Velocidad:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${currentSpeed.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Slider(
                        value = currentSpeed,
                        onValueChange = { newSpeed ->
                            currentSpeed = newSpeed
                        },
                        onValueChangeFinished = {
                            // Enviar velocidad al servidor cuando se suelta el slider
                            coroutineScope.launch {
                                LineFollowManager.setSpeed(currentSpeed.toInt())
                            }
                        },
                        valueRange = 10f..100f,
                        steps = 17, // Pasos de 5 en 5
                        enabled = isConnected && !isFollowing, // Deshabilitado mientras sigue la línea
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rápido", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()

                // Botones de control principal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón INICIAR
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                statusText = "🚀 Iniciando seguimiento..."
                                val result = LineFollowManager.start()

                                result.onSuccess { message ->
                                    isFollowing = true
                                    statusText = "🟢 Siguiendo línea..."
                                    Log.d("LineFollow", "Iniciado: $message")
                                }.onFailure { error ->
                                    statusText = "❌ Error: ${error.message}"
                                    Log.e("LineFollow", "Error iniciando: ${error.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected && isCalibrated && !isFollowing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("INICIAR")
                    }

                    // Botón PARAR
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                statusText = "⏹️ Deteniendo..."
                                val result = LineFollowManager.stop()

                                result.onSuccess { message ->
                                    isFollowing = false
                                    statusText = "⏸️ Detenido"
                                    Log.d("LineFollow", "Detenido: $message")
                                }.onFailure { error ->
                                    statusText = "❌ Error: ${error.message}"
                                    Log.e("LineFollow", "Error deteniendo: ${error.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected && isFollowing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("PARAR")
                    }
                }

                // Botón CALIBRAR (abajo, ancho completo)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCalibrating = true
                            statusText = "🔧 Calibrando sensores... El robot se moverá automáticamente"

                            val result = LineFollowManager.calibrate()

                            result.onSuccess { message ->
                                isCalibrating = false
                                isCalibrated = true
                                statusText = "✅ Calibración completada - Listo para iniciar"
                                Log.d("LineFollow", "Calibrado: $message")
                            }.onFailure { error ->
                                isCalibrating = false
                                statusText = "❌ Error en calibración: ${error.message}"
                                Log.e("LineFollow", "Error calibrando: ${error.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected && !isFollowing && !isCalibrating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCalibrating) "CALIBRANDO..." else if (isCalibrated) "RECALIBRAR SENSORES" else "CALIBRAR SENSORES")
                }
            }
        }

        // Instrucciones compactas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "💡 Uso rápido:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text("1️⃣ Calibrar → 2️⃣ Ajustar velocidad → 3️⃣ Iniciar", fontSize = 13.sp)
            }
        }
    }
}

