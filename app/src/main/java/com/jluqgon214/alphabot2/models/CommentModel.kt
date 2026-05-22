package com.jluqgon214.alphabot2.models

/**
 * Modelo de comentario asociado a una publicación.
 *
 * Se guarda en la subcolección `comentarios` dentro de cada documento de `posts`.
 *
 * @param id Identificador único del comentario.
 * @param postId ID del post al que pertenece.
 * @param userId ID del autor del comentario.
 * @param username Nombre visible del autor.
 * @param userAvatarUrl URL del avatar del autor.
 * @param text Texto del comentario.
 * @param createdAt Marca temporal en milisegundos (epoch).
 * @param likesCount Número total de likes del comentario.
 * @param likedBy Lista de IDs de usuarios que han dado like.
 */
data class CommentModel(
    val id: String,
    val postId: String,
    val userId: String,
    val username: String,
    val userAvatarUrl: String,
    val text: String,
    val createdAt: Long,
    val likesCount: Int,
    val likedBy: List<String>
)
