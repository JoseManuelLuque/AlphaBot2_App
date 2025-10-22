package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jluqgon214.alphabot2.network.SSHManager
import kotlinx.coroutines.launch

data class Song(
    val name: String,
    val icon: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuzzerScreen(
    host: String,
    user: String,
    password: String
) {
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Lista de canciones disponibles
    val songs = listOf(
        Song("Star Wars", "🌟", "Tema principal de Star Wars"),
        Song("Happy Birthday", "🎂", "Cumpleaños feliz"),
        Song("Super Mario", "🍄", "Tema de Super Mario Bros"),
        Song("Take On Me", "🎸", "A-ha - Take On Me"),
        Song("Nokia Ringtone", "📱", "Clásico tono Nokia"),
        Song("Tetris", "🎮", "Tema de Tetris"),
        Song("Imperial March", "⚫", "La Marcha Imperial - Star Wars"),
        Song("Jingle Bells", "🔔", "Navidad - Jingle Bells")
    )

    fun playBuzzer(command: String, description: String) {
        coroutineScope.launch {
            isPlaying = true
            statusMessage = "🔊 $description"

            SSHManager.executeCommand("sudo python3 '/home/pi/Android App/buzzer_control.py' $command 2>&1") { result ->
                isPlaying = false
                if (result.contains("ERROR") || result.contains("error")) {
                    statusMessage = "❌ Error al reproducir"
                } else {
                    statusMessage = "✅ Reproducido correctamente"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "🎵 Control del Buzzer",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Reproduce pitidos y melodías",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Estado
        if (statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusMessage.contains("❌"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Pitidos simples
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔔 Pitidos Simples",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pitido corto
                    Button(
                        onClick = { playBuzzer("beep_short", "Pitido corto") },
                        enabled = !isPlaying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Corto")
                        }
                    }

                    // Pitido largo
                    Button(
                        onClick = { playBuzzer("beep_long", "Pitido largo") },
                        enabled = !isPlaying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Largo")
                        }
                    }

                    // Doble pitido
                    Button(
                        onClick = { playBuzzer("beep_double", "Doble pitido") },
                        enabled = !isPlaying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Doble")
                        }
                    }
                }
            }
        }

        // Lista de canciones
        Text(
            text = "🎼 Melodías",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs.size) { index ->
                val song = songs[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        playBuzzer("song_${song.name.lowercase().replace(" ", "_")}", "Reproduciendo ${song.name}")
                    },
                    enabled = !isPlaying
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = song.icon,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = song.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = song.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

