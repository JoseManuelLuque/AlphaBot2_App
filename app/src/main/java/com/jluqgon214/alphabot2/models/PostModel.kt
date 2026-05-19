package com.jluqgon214.alphabot2.models

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
