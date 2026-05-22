package com.jluqgon214.alphabot2.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.jluqgon214.alphabot2.viewmodels.AuthViewModel

/**
 * Pantalla de registro de nuevos usuarios.
 *
 * Permite crear una cuenta proporcionando:
 * - Correo electrónico
 * - Nombre de usuario
 * - Contraseña (con confirmación)
 *
 * Valida que las contraseñas coincidan y que todos los campos estén rellenos.
 *
 * @param navController NavController para navegar entre pantallas.
 * @param authViewModel ViewModel que gestiona el registro.
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "Crear una Cuenta",
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

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && username.isNotBlank()) {
                            if (password == confirmPassword) {
                                authViewModel.register(email, password, username)
                            } else {
                                errorMessage = "Las contraseñas no coinciden."
                            }
                        } else {
                            errorMessage = "Por favor, rellena todos los campos."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Registrarse",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Observa el estado de autenticación y navega cuando esté autenticado
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthViewModel.AuthState.Authenticated -> {
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

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("¿Ya tienes cuenta? Inicia Sesión")
                }
            }
        }
    }
}
