package com.jluqgon214.alphabot2.Screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jluqgon214.alphabot2.SSHManager
import com.jluqgon214.alphabot2.executePythonScript
import com.jluqgon214.alphabot2.stopPythonScript

@Composable
fun MainScreen(host: String, user: String, password: String, innerPadding: PaddingValues) {
    var outputText by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        SSHManager.connect(host, user, password) {
            isConnected = it
            outputText += if (it) "Conectado correctamente\n" else "Error en la conexión\n"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SSHManager.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = {
            executePythonScript("AlphaBot2.py") { line ->
                outputText += line + "\n"
            }
        }, enabled = isConnected) {
            Text("Iniciar Script")
        }

        Button(onClick = {
            stopPythonScript { 
                outputText += "Script detenido\n"
            }
        }, enabled = isConnected) {
            Text("Parar Script")
        }

        Text(text = outputText, modifier = Modifier.fillMaxWidth())
    }
}