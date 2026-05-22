package com.jluqgon214.alphabot2.models

/**
 * Modelo de una publicación del feed social.
 *
 * Este objeto representa el documento principal guardado en la colección
 * `posts` de Firestore.
 *
 * @param id Identificador único del post.
 * @param userId ID del usuario autor.
 * @param username Nombre visible del autor en el feed.
 * @param userAvatarUrl URL del avatar del autor.
 * @param title Título opcional del post.
 * @param text Texto principal del post.
 * @param imageUrl URL de imagen asociada (vacía si no hay imagen).
 * @param createdAt Marca temporal en milisegundos (epoch).
 * @param likesCount Número total de likes.
 * @param likedBy Lista de IDs de usuarios que han dado like.
 */
data class PostModel(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatarUrl: String,
    val title: String,
    val text: String,
    val imageUrl: String,
    val createdAt: Long,
    val likesCount: Int,
    val likedBy: List<String>
)
