package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.jluqgon214.alphabot2.network.LedManager
import com.jluqgon214.alphabot2.network.SSHManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LedsScreen(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues
) {
    var statusText by remember { mutableStateOf("Iniciando...") }
    var isConnected by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var brightness by remember { mutableStateOf(100f) }
    var isOn by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Conectar al servidor de LEDs al iniciar
    LaunchedEffect(Unit) {
        SSHManager.connect(host, user, password) { sshConnected ->
            if (sshConnected) {
                statusText = "SSH conectado. Iniciando servidor de LEDs..."

                // Iniciar servidor de LEDs
                SSHManager.executeCommand("sudo pkill -f led_server.py") { _ ->
                    coroutineScope.launch {
                        delay(500)
                        SSHManager.executeCommand("sudo nohup python3 '/home/pi/Android App/led_server.py' > /tmp/led_server.log 2>&1 &") { _ ->
                            statusText = "Servidor de LEDs iniciado. Conectando..."

                            coroutineScope.launch {
                                delay(2000)

                                LedManager.connect(host) { connected ->
                                    isConnected = connected
                                    statusText = if (connected) {
                                        "✅ Conectado - LEDs listos"
                                    } else {
                                        "❌ Error conectando al servidor de LEDs"
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
            .padding(16.dp)
            .verticalScroll(scrollState),
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
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
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
                    onCheckedChange = { newState ->
                        if (isConnected) {
                            isOn = newState
                            if (newState) {
                                LedManager.turnOn()
                            } else {
                                LedManager.turnOff()
                            }
                        }
                    },
                    enabled = isConnected
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
                                    onClick = {
                                        if (isConnected) {
                                            selectedColor = color
                                            // Enviar color al servidor
                                            val red = (color.red * 255).toInt()
                                            val green = (color.green * 255).toInt()
                                            val blue = (color.blue * 255).toInt()
                                            LedManager.setColor(red, green, blue)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedColor == color)
                                            color.copy(alpha = 0.8f)
                                        else
                                            color.copy(alpha = 0.3f)
                                    ),
                                    enabled = isConnected
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
                    onValueChange = { newBrightness ->
                        brightness = newBrightness
                    },
                    onValueChangeFinished = {
                        // Enviar brillo al servidor cuando se suelta
                        if (isConnected) {
                            LedManager.setBrightness(brightness.toInt())
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 19, // Pasos de 5%
                    enabled = isConnected,
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

        // Efectos/Animaciones
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✨ Efectos y Animaciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Primera fila de efectos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Estático
                    Button(
                        onClick = {
                            if (isConnected) {
                                LedManager.setEffect("static")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null)
                            Text("Estático", fontSize = 11.sp)
                        }
                    }

                    // Botón Arcoíris
                    Button(
                        onClick = {
                            if (isConnected) {
                                LedManager.setEffect("rainbow")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null)
                            Text("Arcoíris", fontSize = 11.sp)
                        }
                    }
                }

                // Segunda fila de efectos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Parpadeo
                    Button(
                        onClick = {
                            if (isConnected) {
                                LedManager.setEffect("blink")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null)
                            Text("Parpadeo", fontSize = 11.sp)
                        }
                    }

                    // Botón Respiración
                    Button(
                        onClick = {
                            if (isConnected) {
                                LedManager.setEffect("breathe")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Air, contentDescription = null)
                            Text("Respiración", fontSize = 11.sp)
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

    // Limpieza al salir
    DisposableEffect(Unit) {
        onDispose {
            LedManager.disconnect()
        }
    }
}

