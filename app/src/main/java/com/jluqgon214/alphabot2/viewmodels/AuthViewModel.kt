package com.jluqgon214.alphabot2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

    fun register(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _authState.value = AuthState.Error("Email, password, and username cannot be empty.")
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
                        "email" to email
                    )
                    _firestore.collection("usuarios").document(user.uid).set(userMap).await()
                }
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                _auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
}
