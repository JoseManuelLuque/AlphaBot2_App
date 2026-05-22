package com.jluqgon214.alphabot2.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.jluqgon214.alphabot2.models.CommentModel
import com.jluqgon214.alphabot2.models.PostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.util.UUID
import kotlin.math.max


/**
 * ViewModel del módulo social (posts y comentarios).
 *
 * Se encarga de:
 * - Escuchar posts/comentarios en tiempo real desde Firestore.
 * - Crear publicaciones (con imagen opcional en Storage).
 * - Gestionar likes de posts y comentarios.
 * - Crear comentarios.
 * - Borrar posts propios y limpiar recursos asociados.
 */
class PostsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts: StateFlow<List<PostModel>> = _posts

    private val _commentsByPost = MutableStateFlow<Map<String, List<CommentModel>>>(emptyMap())
    val commentsByPost: StateFlow<Map<String, List<CommentModel>>> = _commentsByPost

    private val _creatingPost = MutableStateFlow(false)
    val creatingPost: StateFlow<Boolean> = _creatingPost

    private val _loadingPosts = MutableStateFlow(true)
    val loadingPosts: StateFlow<Boolean> = _loadingPosts

    private val _sendingCommentByPost = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val sendingCommentByPost: StateFlow<Map<String, Boolean>> = _sendingCommentByPost

    private val _deletingPostIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingPostIds: StateFlow<Set<String>> = _deletingPostIds

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message

    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid.orEmpty())
    val currentUserId: StateFlow<String> = _currentUserId

    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole

    private var postsListener: ListenerRegistration? = null
    private val commentListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        loadUserRole()
        observePosts()
    }

    private fun loadUserRole() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                _userRole.value = doc.getString("role") ?: "user"
            }
    }

    // Escucha los posts en tiempo real ordenados por recientes.
    private fun observePosts() {
        postsListener?.remove()
        postsListener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = error.message ?: "Error cargando posts"
                    _loadingPosts.value = false
                    return@addSnapshotListener
                }

                val loadedPosts = snapshot?.documents?.map { it.toPostModel() }.orEmpty()
                _posts.value = loadedPosts
                syncCommentListeners(loadedPosts.map { it.id }.toSet())
                _loadingPosts.value = false
            }
    }

    // Crea y elimina listeners de comentarios solo para los posts visibles.
    private fun syncCommentListeners(postIds: Set<String>) {
        val obsoleteIds = commentListeners.keys - postIds
        obsoleteIds.forEach { postId ->
            commentListeners.remove(postId)?.remove()
            _commentsByPost.value = _commentsByPost.value - postId
        }

        val missingIds = postIds - commentListeners.keys
        missingIds.forEach { postId ->
            val listener = firestore.collection("posts").document(postId)
                .collection("comentarios")
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = error.message ?: "Error cargando comentarios"
                        return@addSnapshotListener
                    }

                    val comments = snapshot?.documents
                        ?.map { it.toCommentModel(postId) }
                        ?.sortedWith(compareByDescending<CommentModel> { it.likesCount }.thenByDescending { it.createdAt })
                        .orEmpty()
                    _commentsByPost.value = _commentsByPost.value.toMutableMap().apply {
                        this[postId] = comments
                    }
                }

            commentListeners[postId] = listener
        }
    }

    /** Limpia el error actual para que la UI deje de mostrarlo. */
    fun clearError() {
        _error.value = null
    }

    /**
     * Crea una publicación nueva.
     *
     * @param title Título opcional (máx. 120).
     * @param text Texto opcional (máx. 2000).
     * @param imageUri URI de imagen local opcional.
     * @param onSuccess Callback ejecutado tras creación correcta.
     */
    fun createPost(title: String, text: String, imageUri: Uri?, onSuccess: () -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: run {
            _error.value = "Debes iniciar sesión para publicar"
            return
        }

        val cleanTitle = title.trim()
        val cleanText = text.trim()
        if (cleanTitle.isBlank() && cleanText.isBlank()) {
            _error.value = "Escribe al menos un título o texto"
            return
        }
        if (cleanTitle.length > 120) {
            _error.value = "El título no puede superar 120 caracteres"
            return
        }
        if (cleanText.length > 2000) {
            _error.value = "El texto no puede superar 2000 caracteres"
            return
        }

        _creatingPost.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("usuarios").document(uid).get().await()
                val username = userDoc.getString("username")
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: "Usuario"
                val userAvatarUrl = userDoc.getString("avatarUrl") ?: ""

                val imageUrl = if (imageUri != null) {
                    val imagePath = "posts/$uid/${UUID.randomUUID()}.jpg"
                    val imageRef = storage.reference.child(imagePath)
                    imageRef.putFile(imageUri).await()
                    imageRef.downloadUrl.await().toString()
                } else {
                    ""
                }

                val postRef = firestore.collection("posts").document()
                val now = System.currentTimeMillis()

                val postData = mapOf(
                    "id" to postRef.id,
                    "userId" to uid,
                    "username" to username,
                    "userAvatarUrl" to userAvatarUrl,
                    "title" to cleanTitle,
                    "text" to cleanText,
                    "imageUrl" to imageUrl,
                    "createdAt" to now,
                    "likesCount" to 0,
                    "likedBy" to emptyList<String>()
                )

                postRef.set(postData).await()
                _message.tryEmit("Publicación creada")
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error publicando"
            } finally {
                _creatingPost.value = false
            }
        }
    }

    /**
     * Alterna el like del usuario actual sobre un post.
     *
     * @param postId ID del post objetivo.
     */
    fun togglePostLike(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(postId)

        viewModelScope.launch {
            try {
                firestore.runTransaction { tx ->
                    val snapshot = tx.get(postRef)
                    val likedBy = (snapshot.get("likedBy") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    val currentLikes = (snapshot.getLong("likesCount") ?: 0L).toInt()

                    val alreadyLiked = likedBy.contains(uid)
                    val newLikedBy = if (alreadyLiked) likedBy - uid else likedBy + uid
                    val newLikes = if (alreadyLiked) max(0, currentLikes - 1) else currentLikes + 1

                    tx.update(postRef, mapOf("likedBy" to newLikedBy, "likesCount" to newLikes))
                    null
                }.await()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al dar me gusta"
            }
        }
    }

    /**
     * Añade un comentario a un post.
     *
     * @param postId ID del post a comentar.
     * @param text Texto del comentario (máx. 500).
     */
    fun addComment(postId: String, text: String) {
        val uid = auth.currentUser?.uid ?: run {
            _error.value = "Debes iniciar sesión para comentar"
            return
        }

        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        if (cleanText.length > 500) {
            _error.value = "El comentario no puede superar 500 caracteres"
            return
        }

        _sendingCommentByPost.value = _sendingCommentByPost.value.toMutableMap().apply {
            this[postId] = true
        }

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("usuarios").document(uid).get().await()
                val username = userDoc.getString("username")
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: "Usuario"
                val userAvatarUrl = userDoc.getString("avatarUrl") ?: ""

                val commentRef = firestore.collection("posts").document(postId)
                    .collection("comentarios")
                    .document()

                val commentData = mapOf(
                    "id" to commentRef.id,
                    "postId" to postId,
                    "userId" to uid,
                    "username" to username,
                    "userAvatarUrl" to userAvatarUrl,
                    "text" to cleanText,
                    "createdAt" to System.currentTimeMillis(),
                    "likesCount" to 0,
                    "likedBy" to emptyList<String>()
                )

                commentRef.set(commentData).await()
                _message.tryEmit("Comentario publicado")
            } catch (e: Exception) {
                _error.value = e.message ?: "Error creando comentario"
            } finally {
                _sendingCommentByPost.value = _sendingCommentByPost.value.toMutableMap().apply {
                    this[postId] = false
                }
            }
        }
    }

    /**
     * Alterna el like del usuario actual sobre un comentario.
     *
     * @param postId ID del post padre.
     * @param commentId ID del comentario objetivo.
     */
    fun toggleCommentLike(postId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val commentRef = firestore.collection("posts").document(postId)
            .collection("comentarios")
            .document(commentId)

        viewModelScope.launch {
            try {
                firestore.runTransaction { tx ->
                    val snapshot = tx.get(commentRef)
                    val likedBy = (snapshot.get("likedBy") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    val currentLikes = (snapshot.getLong("likesCount") ?: 0L).toInt()

                    val alreadyLiked = likedBy.contains(uid)
                    val newLikedBy = if (alreadyLiked) likedBy - uid else likedBy + uid
                    val newLikes = if (alreadyLiked) max(0, currentLikes - 1) else currentLikes + 1

                    tx.update(commentRef, mapOf("likedBy" to newLikedBy, "likesCount" to newLikes))
                    null
                }.await()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al dar me gusta al comentario"
            }
        }
    }

    /**
     * Borra un post y sus comentarios.
     *
     * Nota: por seguridad esta implementación solo permite borrar posts propios.
     *
     * @param post Publicación a eliminar.
     */
    fun deletePost(post: PostModel) {
        val uid = auth.currentUser?.uid ?: return
        if (post.userId != uid) {
            _error.value = "Solo puedes borrar tus propios posts"
            return
        }

        _deletingPostIds.value = _deletingPostIds.value + post.id

        viewModelScope.launch {
            try {
                val postRef = firestore.collection("posts").document(post.id)

                // Borramos todos los comentarios del post en bloques (máx 500 operaciones por batch).
                while (true) {
                    val comments = postRef.collection("comentarios").limit(400).get().await().documents
                    if (comments.isEmpty()) break

                    val batch = firestore.batch()
                    comments.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }

                postRef.delete().await()

                // Intento de borrado de imagen del post si existe.
                val imagePath = extractStoragePath(post.imageUrl)
                if (!imagePath.isNullOrBlank()) {
                    try {
                        storage.reference.child(imagePath).delete().await()
                    } catch (_: Exception) {
                        // Si falla la limpieza en Storage no bloqueamos el borrado del post.
                    }
                }

                _message.tryEmit("Post eliminado")
            } catch (e: Exception) {
                _error.value = e.message ?: "Error borrando el post"
            } finally {
                _deletingPostIds.value = _deletingPostIds.value - post.id
            }
        }
    }

    /**
     * Extrae la ruta interna de Firebase Storage desde una URL de descarga pública.
     *
     * @param url URL pública de Storage.
     * @return Ruta interna (por ejemplo `posts/uid/archivo.jpg`) o `null`.
     */
    private fun extractStoragePath(url: String): String? {
        if (url.isBlank()) return null
        val after = url.substringAfter("/o/", "")
        if (after.isBlank()) return null
        return try {
            URLDecoder.decode(after.substringBefore("?"), "UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    /** Libera listeners de Firestore al destruirse el ViewModel. */
    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
        commentListeners.values.forEach { it.remove() }
        commentListeners.clear()
    }
}

/** Convierte un documento de Firestore en el modelo de post de la app. */
private fun DocumentSnapshot.toPostModel(): PostModel {
    return PostModel(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        username = getString("username") ?: "Usuario",
        userAvatarUrl = getString("userAvatarUrl") ?: "",
        title = getString("title") ?: "",
        text = getString("text") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        likesCount = (getLong("likesCount") ?: 0L).toInt(),
        likedBy = (get("likedBy") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    )
}

/** Convierte un documento de Firestore en el modelo de comentario de la app. */
private fun DocumentSnapshot.toCommentModel(postId: String): CommentModel {
    return CommentModel(
        id = getString("id") ?: id,
        postId = postId,
        userId = getString("userId") ?: "",
        username = getString("username") ?: "Usuario",
        userAvatarUrl = getString("userAvatarUrl") ?: "",
        text = getString("text") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        likesCount = (getLong("likesCount") ?: 0L).toInt(),
        likedBy = (get("likedBy") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    )
}


