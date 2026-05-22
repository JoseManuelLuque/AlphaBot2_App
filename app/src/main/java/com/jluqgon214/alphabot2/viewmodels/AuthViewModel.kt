package com.jluqgon214.alphabot2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel de autenticación.
 *
 * Gestiona login/registro con correo y Google, y valida bloqueos de cuenta
 * definidos en Firestore antes de permitir la entrada.
 */
class AuthViewModel : ViewModel() {
    private val _auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Estados de UI para el flujo de autenticación. */
    sealed class AuthState {
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
        object Loading : AuthState()
        object Unauthenticated : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    // Traduce los mensajes de error de Firebase al español
    private fun traducirErrorFirebase(errorMessage: String?): String {
        return when {
            errorMessage == null -> "Ha ocurrido un error desconocido."
            errorMessage.contains("There is no user record") -> "No existe una cuenta con este correo electrónico."
            errorMessage.contains("The password is invalid") -> "La contraseña es incorrecta."
            errorMessage.contains("The email address is already in use") -> "Este correo electrónico ya está registrado."
            errorMessage.contains("Password should be at least 6 characters") -> "La contraseña debe tener al menos 6 caracteres."
            errorMessage.contains("weak-password") -> "La contraseña es demasiado débil. Usa letras, números y símbolos."
            errorMessage.contains("invalid-email") -> "El formato del correo electrónico no es válido."
            errorMessage.contains("user-disabled") -> "Esta cuenta ha sido deshabilitada."
            errorMessage.contains("too-many-requests") -> "Demasiados intentos fallidos. Intenta más tarde."
            errorMessage.contains("operation-not-allowed") -> "Operación no permitida. Contacta con soporte."
            errorMessage.contains("network") -> "Error de conexión. Comprueba tu internet."
            else -> errorMessage
        }
    }

    // Comprueba si la cuenta sigue bloqueada y devuelve el mensaje si no puede entrar.
    private suspend fun comprobarBloqueo(userId: String): String? {
        val userDoc = _firestore.collection("usuarios").document(userId).get().await()
        val blockedUntil = userDoc.getLong("blockedUntil") ?: 0L
        val blockReason = userDoc.getString("blockReason") ?: ""

        return if (blockedUntil > System.currentTimeMillis()) {
            val remainingMs = blockedUntil - System.currentTimeMillis()
            val hours = remainingMs / (1000 * 60 * 60)
            "Cuenta bloqueada. Te queda: $hours horas. Razón: $blockReason"
        } else {
            null
        }
    }

    /**
     * Valida una sesión ya guardada en Firebase Auth.
     *
     * Si la cuenta está bloqueada, fuerza cierre de sesión y devuelve error a la UI.
     */
    fun validarSesionGuardada() {
        val currentUser = _auth.currentUser ?: return
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val mensajeBloqueo = comprobarBloqueo(currentUser.uid)
                if (mensajeBloqueo != null) {
                    _auth.signOut()
                    _authState.value = AuthState.Error(mensajeBloqueo)
                } else {
                    _authState.value = AuthState.Authenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(traducirErrorFirebase(e.message))
            }
        }
    }

    /**
     * Completa la autenticación con Google usando el `idToken` recibido por la UI.
     *
     * @param idToken Token OAuth emitido por Google Sign-In.
     */
    fun loginWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = _auth.signInWithCredential(credential).await()
                val user = result.user

                if (user != null) {
                    // Verificar si el usuario está bloqueado.
                    val mensajeBloqueo = comprobarBloqueo(user.uid)

                    if (mensajeBloqueo != null) {
                        _auth.signOut()
                        _authState.value = AuthState.Error(mensajeBloqueo)
                    } else {
                        // Si es la primera vez, crear documento en Firestore.
                        val userDoc =
                            _firestore.collection("usuarios").document(user.uid).get().await()
                        if (!userDoc.exists()) {
                            val userMap = hashMapOf(
                                "username" to (user.displayName ?: user.email ?: "Usuario"),
                                "email" to (user.email ?: ""),
                                "role" to "user"
                            )
                            _firestore.collection("usuarios").document(user.uid).set(userMap)
                                .await()
                        }
                        _authState.value = AuthState.Authenticated
                    }
                } else {
                    _authState.value = AuthState.Error("Error en la autenticación con Google.")
                }
            } catch (e: Exception) {
                _authState.value =
                    AuthState.Error(traducirErrorFirebase(e.message))
            }
        }
    }

    /**
     * Registra un nuevo usuario con correo/contraseña y crea su documento base en Firestore.
     *
     * @param email Correo del usuario.
     * @param password Contraseña del usuario.
     * @param username Nombre visible del perfil.
     */
    fun register(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _authState.value =
                AuthState.Error("El correo, contraseña y nombre de usuario no pueden estar vacíos.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = _auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "role" to "user"  // Por defecto, nuevo usuario es "user"
                    )
                    _firestore.collection("usuarios").document(user.uid).set(userMap).await()
                }
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(traducirErrorFirebase(e.message))
            }
        }
    }

    /**
     * Inicia sesión con correo/contraseña y aplica control de bloqueo de cuenta.
     *
     * @param email Correo del usuario.
     * @param password Contraseña del usuario.
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("El correo y la contraseña no pueden estar vacíos.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = _auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid

                if (userId != null) {
                    val mensajeBloqueo = comprobarBloqueo(userId)

                    if (mensajeBloqueo != null) {
                        // Si está bloqueado, cerramos sesión y no le dejamos pasar.
                        _auth.signOut()
                        _authState.value = AuthState.Error(mensajeBloqueo)
                    } else {
                        _authState.value = AuthState.Authenticated
                    }
                } else {
                    _authState.value = AuthState.Authenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(traducirErrorFirebase(e.message))
            }
        }
    }
}
