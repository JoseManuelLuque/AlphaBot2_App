package com.jluqgon214.alphabot2.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.jluqgon214.alphabot2.utils.DataStoreManager
import com.jluqgon214.alphabot2.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remember by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val dataStoreManager = remember(context) { DataStoreManager(context) }
    val rememberedEmail by dataStoreManager.rememberedEmailFlow.collectAsState(initial = "")
    val rememberPref by dataStoreManager.rememberMeFlow.collectAsState(initial = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🤖 AlphaBot2",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Iniciar Sesión",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Checkbox para recordar el usuario
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remember, onCheckedChange = { remember = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Recordarme")
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            authViewModel.login(email, password)
                        } else {
                            errorMessage = "Por favor, rellena todos los campos."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Entrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthViewModel.AuthState.Authenticated -> {
                            // Guardamos o limpiamos "Recordarme" en DataStore
                            val emailToSave = if (remember) email else null
                            dataStoreManager.saveRememberMe(email = emailToSave, remember = remember)

                            navController.navigate("config") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        is AuthViewModel.AuthState.Error -> {
                            errorMessage = (authState as AuthViewModel.AuthState.Error).message
                        }
                        else -> Unit
                    }
                }

                // Carga los valores guardados para dejar el login relleno como lo tenía el usuario.
                LaunchedEffect(rememberPref, rememberedEmail) {
                    remember = rememberPref
                    if (remember && email.isBlank() && rememberedEmail.isNotBlank()) {
                        email = rememberedEmail
                    }
                }

                // Si hay sesión guardada y el usuario marcó "Recordarme", la validamos al entrar.
                LaunchedEffect(rememberPref) {
                    if (rememberPref) {
                        authViewModel.validarSesionGuardada()
                    }
                }

                TextButton(onClick = { navController.navigate("register") }) {
                    Text("¿No tienes cuenta? Regístrate")
                }
            }
        }
    }
}
