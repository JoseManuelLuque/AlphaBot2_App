package com.jluqgon214.alphabot2.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder

/**
 * ViewModel del perfil de usuario y funciones administrativas.
 *
 * Gestiona datos del usuario logueado (nombre, email, avatar), acciones de cuenta
 * (editar, borrar) y herramientas de administración (promoción, bloqueo y eliminación
 * de otros usuarios).
 */
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

    private val _role = MutableStateFlow("")
    val role: StateFlow<String> = _role

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val _bloqueos = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val bloqueos: StateFlow<List<Map<String, Any>>> = _bloqueos

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

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
                        _role.value = document.getString("role") ?: "user"
                    }
                }
        }
    }

    /** Actualiza el nombre visible del usuario actual en Firestore. */
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
                _error.value = e.message ?: "Error actualizando nombre de usuario"
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
            // Leer avatar previo para limpiar archivos antiguos si existieran.
            val docRef = firestore.collection("usuarios").document(uid)
            val existingDoc = try { docRef.get().await() } catch (t: Exception) { null }

            val existingPath: String? = existingDoc?.getString("avatarPath") ?: existingDoc?.getString("avatarUrl")?.let { url ->
                // Intentar extraer la ruta interna de Storage desde la URL pública.
                val after = url.substringAfter("/o/", "")
                if (after.isNotEmpty()) {
                    try {
                        URLDecoder.decode(after.substringBefore("?"), "UTF-8")
                    } catch (_: Exception) { null }
                } else null
            }

            // Ruta estable por usuario para evitar duplicados de avatar.
            val newPath = "avatars/$uid.jpg"

            // Si la ruta previa es distinta, intentar borrar el archivo antiguo.
            if (!existingPath.isNullOrEmpty() && existingPath != newPath) {
                try {
                    storage.reference.child(existingPath).delete().await()
                } catch (_: Exception) {
                    // Ignoramos el fallo de limpieza para no bloquear la subida nueva.
                }
            }

            // Subir avatar a Storage en avatars/{uid}.jpg.
            val ref = storage.reference.child(newPath)
            ref.putFile(uri).await()

            // Guardar URL pública y ruta interna en Firestore.
            val downloadUrl = ref.downloadUrl.await().toString()
            val data = mapOf(
                "avatarUrl" to downloadUrl,
                "avatarPath" to newPath
            )
            firestore.collection("usuarios").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            _avatarUrl.value = downloadUrl
        } catch (e: Exception) {
            _error.value = e.message ?: "Error subiendo avatar"
        } finally {
            _loading.value = false
        }
    }

    /** Limpia el mensaje de error actual para que la UI deje de mostrarlo. */
    fun clearError() {
        _error.value = null
    }

    // ============ FUNCIONES DE ADMIN ============
    /**
     * Eleva al usuario actual a rol administrador tras validar contraseña interna.
     *
     * @param password Clave de acceso al modo admin.
     */
    fun promoteToAdmin(password: String) {
        val uid = auth.currentUser?.uid ?: return
        val adminPassword = "admin"  // Contraseña hardcodeada (en producción, usar backend)

        _loading.value = true
        _error.value = null

        if (password != adminPassword) {
            _error.value = "Contraseña incorrecta"
            _loading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val data = mapOf("role" to "admin")
                firestore.collection("usuarios").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                _role.value = "admin"
            } catch (e: Exception) {
                _error.value = e.message ?: "Error asignando rol de administrador"
            } finally {
                _loading.value = false
            }
        }
    }

    // Bloquear usuario por X horas
    /**
     * Bloquea a un usuario durante un número de horas y registra el evento en logs.
     *
     * @param userId Usuario objetivo del bloqueo.
     * @param hours Duración del bloqueo.
     * @param reason Motivo del bloqueo.
     */
    fun blockUser(userId: String, hours: Long, reason: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val blockedUntil = System.currentTimeMillis() + (hours * 60 * 60 * 1000)

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Actualizar estado de bloqueo en el usuario
                val data = mapOf(
                    "blockedUntil" to blockedUntil,
                    "blockReason" to reason
                )
                firestore.collection("usuarios").document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()

                // Guardar log en subcollection
                val logData = mapOf(
                    "fechaBloqueo" to System.currentTimeMillis(),
                    "adminId" to currentUid,
                    "adminNombre" to (_username.value.ifBlank { "Admin" }),
                    "horasBloqueadas" to hours,
                    "razon" to reason,
                    "desbloqueadoEn" to 0L
                )
                firestore.collection("usuarios").document(userId)
                    .collection("bloqueos")
                    .add(logData)
                    .await()

                // Refrescar logs localmente y notificar a la UI
                loadBloqueos(userId)
                _events.emit("blocked:$userId")
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error bloqueando usuario"
            } finally {
                _loading.value = false
            }
        }
    }

    // Desbloquear usuario
    /**
     * Elimina el bloqueo activo de un usuario.
     *
     * @param userId Usuario a desbloquear.
     */
    fun unblockUser(userId: String) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "blockedUntil" to 0L,
                    "blockReason" to ""
                )
                firestore.collection("usuarios").document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                // Refrescar y notificar
                loadBloqueos(userId)
                _events.emit("unblocked:$userId")
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desbloqueando usuario"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Obtiene el perfil público de otro usuario.
     *
     * @param userId Usuario del que se consulta el perfil.
     * @return Mapa de campos de Firestore o `null` si falla la consulta.
     */
    suspend fun fetchUserProfile(userId: String): Map<String, Any>? {
        return try {
            firestore.collection("usuarios").document(userId).get().await().data
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina la cuenta de un usuario (función de administración).
     *
     * @param userId Usuario a eliminar.
     */
    fun deleteUser(userId: String) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Borrar documento de usuario
                firestore.collection("usuarios").document(userId).delete().await()
                _events.emit("deleted:$userId")
                // Aquí idealmente también borrar posts del usuario, etc.
            } catch (e: Exception) {
                _error.value = e.message ?: "Error eliminando usuario"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Carga el historial de bloqueos de un usuario ordenado por fecha descendente.
     *
     * @param userId Usuario del que se cargan los logs.
     */
    fun loadBloqueos(userId: String) {
        viewModelScope.launch {
            try {
                val bloqueosSnapshot = firestore.collection("usuarios").document(userId)
                    .collection("bloqueos")
                    .orderBy("fechaBloqueo", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val bloqueosList = bloqueosSnapshot.documents.map { doc ->
                    doc.data ?: emptyMap()
                }
                _bloqueos.value = bloqueosList
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando logs"
            }
        }
    }

    /**
     * Borra completamente la cuenta del usuario actual:
     * documento Firestore, avatar en Storage y usuario en Firebase Auth.
     */
    fun deleteAccount() {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Leer avatar actual para intentar limpiar Storage.
                val avatarDoc = firestore.collection("usuarios").document(uid).get().await()
                val avatarUrl = avatarDoc.getString("avatarUrl")

                // Borrar documento en Firestore.
                firestore.collection("usuarios").document(uid).delete().await()

                // Borrar avatar en Storage si existe.
                if (!avatarUrl.isNullOrEmpty()) {
                    try {
                        val ref = storage.reference.child("avatars/$uid.jpg")
                        ref.delete().await()
                    } catch (_: Exception) {
                        // Ignoramos error de limpieza para no bloquear borrado de cuenta.
                    }
                }

                // Borrar usuario en Firebase Auth.
                val user = auth.currentUser
                if (user != null) {
                    user.delete().await()
                }

                _deleted.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error eliminando cuenta. Puede que debas volver a autenticarte."
            } finally {
                _loading.value = false
            }
        }
    }
}
