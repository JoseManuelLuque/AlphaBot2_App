package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jluqgon214.alphabot2.models.CommentModel
import com.jluqgon214.alphabot2.models.PostModel
import com.jluqgon214.alphabot2.viewmodels.PostsViewModel
import com.jluqgon214.alphabot2.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla para ver el perfil de otro usuario y sus posts.
 * Los admins pueden bloquear, desbloquear y eliminar cuentas.
 */
@Composable
fun UserProfileScreen(
    userId: String,
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    postsViewModel: PostsViewModel = viewModel()
) {
    val userRole by profileViewModel.role.collectAsState()
    val posts by postsViewModel.posts.collectAsState()
    val currentUserId by postsViewModel.currentUserId.collectAsState()
    val loading by profileViewModel.loading.collectAsState()
    val error by profileViewModel.error.collectAsState()
    val bloqueos by profileViewModel.bloqueos.collectAsState()

    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var blockHours by remember { mutableStateOf("24") }
    var blockReason by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Cargar perfil del usuario
    LaunchedEffect(userId) {
        val profile = profileViewModel.fetchUserProfile(userId)
        userProfile = profile
        if (userRole == "admin") {
            profileViewModel.loadBloqueos(userId)
        }
    }

    // Escuchar eventos de acciones (block/unblock/delete) para refrescar la vista en caliente
    LaunchedEffect(Unit) {
        profileViewModel.events.collect { ev ->
            when {
                ev == "deleted:$userId" -> {
                    // Si el usuario fuera eliminado mientras estamos viendo su perfil, volvemos atrás
                    navController.popBackStack()
                }
                ev.startsWith("blocked:") || ev.startsWith("unblocked:") -> {
                    val parts = ev.split(":")
                    if (parts.size == 2 && parts[1] == userId) {
                        // Refrescar datos y logs
                        val profile = profileViewModel.fetchUserProfile(userId)
                        userProfile = profile
                        if (userRole == "admin") profileViewModel.loadBloqueos(userId)
                    }
                }
            }
        }
    }

    val userPosts = posts.filter { it.userId == userId }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Volver")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Perfil de usuario", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(40.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de perfil
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = (userProfile?.get("avatarUrl") as? String).orEmpty()
                                .ifBlank { "https://www.w3schools.com/howto/img_avatar.png" },
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            text = userProfile?.get("username") as? String ?: "Usuario",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = userProfile?.get("email") as? String ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Botones de admin
                        if (userRole == "admin" && userId != currentUserId) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val blockedUntil = (userProfile?.get("blockedUntil") as? Long) ?: 0L
                            val isBlocked = blockedUntil > System.currentTimeMillis()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isBlocked) {
                                    Button(
                                        onClick = { profileViewModel.unblockUser(userId) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !loading
                                    ) {
                                        Text("Desbloquear")
                                    }
                                } else {
                                    Button(
                                        onClick = { showBlockDialog = true },
                                        modifier = Modifier.weight(1f),
                                        enabled = !loading
                                    ) {
                                        Icon(Icons.Default.Block, "Bloquear", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Bloquear")
                                    }
                                }

                                Button(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !loading,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Eliminar")
                                }
                            }

                            if (isBlocked) {
                                val hoursLeft = (blockedUntil - System.currentTimeMillis()) / (1000 * 60 * 60)
                                val reason = userProfile?.get("blockReason") as? String ?: ""
                                Text(
                                    text = "⏳ Bloqueado: $hoursLeft horas. Razón: $reason",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Logs de bloqueo (solo admin) - mostramos SOLO la razón por pedido del usuario
            if (userRole == "admin" && bloqueos.isNotEmpty()) {
                // Contador específico para la razón "usuario problemático"
                val targetReason = "usuario problemático"
                val matches = bloqueos.count { (it["razon"] as? String ?: "").contains(targetReason, ignoreCase = true) }

                item {
                    Text(
                        text = "📋 Historial de bloqueos (${bloqueos.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (matches > 0) {
                    item {
                        Text(
                            text = "⚠️ Atención: bloqueado $matches veces por '$targetReason'",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                // Mostramos solo la razón de cada bloqueo (sin horas)
                items(bloqueos) { bloqueo ->
                    val razon = bloqueo["razon"] as? String ?: "Sin razón"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Razón: $razon",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Posts del usuario
            item {
                Text(
                    text = "Publicaciones (${userPosts.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            if (userPosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "Sin publicaciones",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(userPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        comments = emptyList(),
                        currentUserId = currentUserId,
                        sendingComment = false,
                        deletingPost = false,
                        onTogglePostLike = { postsViewModel.togglePostLike(post.id) },
                        onSendComment = {},
                        onToggleCommentLike = {},
                        onDeletePost = {
                            if (userRole == "admin") {
                                postsViewModel.deletePost(post)
                            }
                        },
                        userRole = userRole,
                        onViewProfile = {}
                    )
                }
            }
        }
    }

    // Diálogo de bloqueo
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Bloquear usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = blockHours,
                        onValueChange = { blockHours = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Horas de bloqueo") },
                        singleLine = true,
                        enabled = !loading
                    )

                    OutlinedTextField(
                        value = blockReason,
                        onValueChange = { if (it.length <= 200) blockReason = it },
                        label = { Text("Razón (opcional)") },
                        minLines = 2,
                        enabled = !loading,
                        supportingText = { Text("${blockReason.length}/200") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (blockHours.isNotBlank()) {
                            profileViewModel.blockUser(userId, blockHours.toLong(), blockReason)
                            showBlockDialog = false
                            blockHours = "24"
                            blockReason = ""
                        }
                    },
                    enabled = !loading && blockHours.isNotBlank()
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de eliminar cuenta
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar cuenta de usuario") },
            text = { Text("¿Estás seguro? Se eliminará la cuenta y todos sus datos. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.deleteUser(userId)
                        showDeleteDialog = false
                        scope.launch {
                            navController.popBackStack()
                        }
                    },
                    enabled = !loading,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

