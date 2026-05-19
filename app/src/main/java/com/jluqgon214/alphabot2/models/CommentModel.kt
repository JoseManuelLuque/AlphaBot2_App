package com.jluqgon214.alphabot2.models

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
