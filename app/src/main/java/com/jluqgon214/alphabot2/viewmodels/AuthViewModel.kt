package com.jluqgon214.alphabot2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val _auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    sealed class AuthState {
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
        object Loading : AuthState()
        object Unauthenticated : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

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

    // Sirve para arrancar la app con la sesión guardada, pero sin saltarnos los bloqueos.
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
                _authState.value = AuthState.Error(e.message ?: "Ha ocurrido un error desconocido.")
            }
        }
    }

    // Completa la autenticación con Google después de que el usuario autoriza.
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
                        val userDoc = _firestore.collection("usuarios").document(user.uid).get().await()
                        if (!userDoc.exists()) {
                            val userMap = hashMapOf(
                                "username" to (user.displayName ?: user.email ?: "Usuario"),
                                "email" to (user.email ?: ""),
                                "role" to "user"
                            )
                            _firestore.collection("usuarios").document(user.uid).set(userMap).await()
                        }
                        _authState.value = AuthState.Authenticated
                    }
                } else {
                    _authState.value = AuthState.Error("Error en la autenticación con Google.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al iniciar sesión con Google.")
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _authState.value = AuthState.Error("Correo, contraseña y nombre de usuario no pueden estar vacíos.")
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
                _authState.value = AuthState.Error(e.message ?: "Ha ocurrido un error desconocido.")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña no pueden estar vacíos.")
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
                _authState.value = AuthState.Error(e.message ?: "Ha ocurrido un error desconocido.")
            }
        }
    }
}
