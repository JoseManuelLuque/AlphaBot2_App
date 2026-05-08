package com.jluqgon214.alphabot2.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl: StateFlow<String> = _avatarUrl

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    init {
        // Al crear el ViewModel cargamos el perfil del usuario actualmente logueado.
        // Esto hace que la pantalla de perfil muestre datos reales desde Firestore.
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // La colección en Firestore se llama "usuarios" (castellano)
            firestore.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Campos guardados en Firestore.
                        // username + email los guardamos al registrarse.
                        // avatarUrl se guarda cuando el usuario sube una foto de perfil.
                        _username.value = document.getString("username") ?: ""
                        _email.value = document.getString("email") ?: ""
                        _avatarUrl.value = document.getString("avatarUrl") ?: ""
                    }
                }
        }
    }

    fun updateUsername(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Usamos SetOptions.merge() para actualizar SOLO el campo indicado
                // sin sobreescribir el resto del documento del usuario.
                val data = mapOf("username" to newName)
                firestore.collection("usuarios").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                _username.value = newName
            } catch (e: Exception) {
                _error.value = e.message ?: "Error updating username"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Sube la imagen de perfil a Firebase Storage y guarda su URL en Firestore.
     *
     * Flujo:
     * 1) Leemos el documento del usuario para saber si existía un avatar anterior.
     *    - Guardamos explícitamente `avatarPath` para poder borrar archivos antiguos.
     * 2) Subimos el nuevo avatar a una ruta estable: `avatars/{uid}.jpg`.
     *    - Ventaja: si el usuario cambia de foto, se sobreescribe el mismo archivo
     *      y NO se llena el Storage con múltiples versiones.
     * 3) Obtenemos la URL de descarga y la guardamos en Firestore (`avatarUrl`).
     */
    suspend fun uploadAvatarAndSave(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null
        try {
            // Read existing avatar path (if any) so we can delete old file if needed
            val docRef = firestore.collection("usuarios").document(uid)
            val existingDoc = try { docRef.get().await() } catch (t: Exception) { null }

            val existingPath: String? = existingDoc?.getString("avatarPath") ?: existingDoc?.getString("avatarUrl")?.let { url ->
                // Try to decode the storage path from a download URL like
                // https://firebasestorage.googleapis.com/v0/b/<bucket>/o/avatars%2Fuid.jpg?alt=media&token=...
                val after = url.substringAfter("/o/", "")
                if (after.isNotEmpty()) {
                    try {
                        URLDecoder.decode(after.substringBefore("?"), "UTF-8")
                    } catch (_: Exception) { null }
                } else null
            }

            // New storage path (we use a stable path per user so uploads overwrite by default)
            val newPath = "avatars/$uid.jpg"

            // If there's an existing different path, try to delete it to avoid orphan files
            if (!existingPath.isNullOrEmpty() && existingPath != newPath) {
                try {
                    storage.reference.child(existingPath).delete().await()
                } catch (_: Exception) {
                    // ignore deletion errors
                }
            }

            // Upload to Storage under avatars/{uid}.jpg
            val ref = storage.reference.child(newPath)
            ref.putFile(uri).await()

            // Get downloadable URL and save it in Firestore (store both URL and path)
            val downloadUrl = ref.downloadUrl.await().toString()
            val data = mapOf(
                "avatarUrl" to downloadUrl,
                "avatarPath" to newPath
            )
            firestore.collection("usuarios").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            _avatarUrl.value = downloadUrl
        } catch (e: Exception) {
            _error.value = e.message ?: "Error uploading avatar"
        } finally {
            _loading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteAccount() {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // First, try to read avatarUrl so we can delete storage object
                val avatarDoc = firestore.collection("usuarios").document(uid).get().await()
                val avatarUrl = avatarDoc.getString("avatarUrl")

                // Delete Firestore document
                firestore.collection("usuarios").document(uid).delete().await()

                // Delete avatar from Storage if exists (we uploaded to avatars/{uid}.jpg)
                if (!avatarUrl.isNullOrEmpty()) {
                    try {
                        val ref = storage.reference.child("avatars/$uid.jpg")
                        ref.delete().await()
                    } catch (_: Exception) {
                        // ignore storage delete errors
                    }
                }

                // Delete auth user
                val user = auth.currentUser
                if (user != null) {
                    user.delete().await()
                }

                _deleted.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting account. You may need to re-authenticate."
            } finally {
                _loading.value = false
            }
        }
    }
}
