package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LedsScreen(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues
) {
    var statusText by remember { mutableStateOf("⚠️ Funcionalidad en desarrollo") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var brightness by remember { mutableStateOf(100f) }
    var isOn by remember { mutableStateOf(false) }

    // Colores predefinidos
    val predefinedColors = listOf(
        Color.Red to "Rojo",
        Color.Green to "Verde",
        Color.Blue to "Azul",
        Color.Yellow to "Amarillo",
        Color.Magenta to "Magenta",
        Color.Cyan to "Cian",
        Color.White to "Blanco"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título
        Text(
            text = "Control de LEDs",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Estado
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

        // Vista previa del LED
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = "💡 Vista Previa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // LED simulado
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = if (isOn) selectedColor.copy(alpha = brightness / 100f) else Color.Gray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOn) Icons.Default.Lightbulb else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (isOn) Color.White else Color.DarkGray,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isOn) "ENCENDIDO" else "APAGADO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Control de encendido/apagado
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estado de LEDs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isOn) "Encendidos" else "Apagados",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOn,
                    onCheckedChange = { isOn = it },
                    enabled = false // Deshabilitado hasta implementar
                )
            }
        }

        // Selector de color
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎨 Seleccionar Color",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Grid de colores
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedColors.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { (color, name) ->
                                Button(
                                    onClick = { selectedColor = color },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedColor == color)
                                            color.copy(alpha = 0.8f)
                                        else
                                            color.copy(alpha = 0.3f)
                                    ),
                                    enabled = false // Deshabilitado hasta implementar
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            // Rellenar espacio si no hay 4 elementos
                            repeat(4 - rowColors.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Control de brillo
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "💫 Brillo:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${brightness.toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = 0f..100f,
                    steps = 19, // Pasos de 5%
                    enabled = false, // Deshabilitado hasta implementar
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Botones de efectos rápidos
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨ Efectos Rápidos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FlashOn, contentDescription = null)
                            Text("Parpadeo", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Colorize, contentDescription = null)
                            Text("Arcoíris", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Instrucciones
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
                    "💡 Instrucciones:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text("1️⃣ Enciende los LEDs con el switch", fontSize = 13.sp)
                Text("2️⃣ Selecciona un color predefinido", fontSize = 13.sp)
                Text("3️⃣ Ajusta el brillo a tu preferencia", fontSize = 13.sp)
            }
        }
    }
}

