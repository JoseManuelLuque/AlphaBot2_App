package com.jluqgon214.alphabot2.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.jluqgon214.alphabot2.utils.DataStoreManager
import com.jluqgon214.alphabot2.viewmodels.AuthViewModel
import com.jluqgon214.alphabot2.BuildConfig

/**
 * Pantalla de inicio de sesión.
 *
 * Permite a los usuarios:
 * - Iniciar sesión con correo y contraseña
 * - Iniciar sesión con Google
 * - Marcar "Recordarme" para autologin en futuras sesiones
 *
 * @param navController NavController para navegar entre pantallas.
 * @param authViewModel ViewModel que gestiona la autenticación.
 */
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

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    val googleSignInClient: GoogleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // resultCode=0 significa que el usuario canceló el diálogo (presionó atrás)
            // Otros códigos indican errores diferentes
            val msg = if (result.resultCode == 0) {
                "Google Sign-In: cancelado por el usuario"
            } else {
                "Google Sign-In: falló con código ${result.resultCode}"
            }
            errorMessage = msg
            return@rememberLauncherForActivityResult
        }

        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                authViewModel.loginWithGoogle(account.idToken!!)
            } else {
                errorMessage = "No se pudo obtener el token de Google"
            }
        } catch (e: ApiException) {
            errorMessage = "Código de error: ${e.statusCode}"
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        }
    }

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

                HorizontalDivider()

                Text(
                    text = "O continua con",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1F2937)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "G",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Continuar con Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
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
