package com.jluqgon214.alphabot2.screens

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

@Composable
fun LineFollowScreen(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues
) {
    var statusText by remember { mutableStateOf("⚠️ Funcionalidad en desarrollo") }
    var isFollowing by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(35f) }
    var isCalibrating by remember { mutableStateOf(false) }

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
                            // TODO: Enviar velocidad al servidor
                        },
                        valueRange = 10f..100f,
                        steps = 17, // Pasos de 5 en 5
                        enabled = false, // Deshabilitado hasta implementar funcionalidad
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
                            // TODO: Implementar inicio de seguimiento
                            isFollowing = true
                            statusText = "🟢 Siguiendo línea... (simulado)"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = false, // Deshabilitado hasta implementar funcionalidad
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
                            // TODO: Implementar detención de seguimiento
                            isFollowing = false
                            statusText = "⏹️ Detenido"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = false, // Deshabilitado hasta implementar funcionalidad
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
                        // TODO: Implementar calibración
                        isCalibrating = true
                        statusText = "🔧 Calibrando sensores... (simulado)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // Deshabilitado hasta implementar funcionalidad
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCalibrating) "CALIBRANDO..." else "CALIBRAR SENSORES")
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

