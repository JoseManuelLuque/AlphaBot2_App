package com.jluqgon214.alphabot2.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jluqgon214.alphabot2.models.CommentModel
import com.jluqgon214.alphabot2.models.PostModel
import com.jluqgon214.alphabot2.navigation.Screen
import com.jluqgon214.alphabot2.viewmodels.PostsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostsScreen(
    postsViewModel: PostsViewModel = viewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val posts by postsViewModel.posts.collectAsState()
    val commentsByPost by postsViewModel.commentsByPost.collectAsState()
    val creatingPost by postsViewModel.creatingPost.collectAsState()
    val loadingPosts by postsViewModel.loadingPosts.collectAsState()
    val sendingCommentByPost by postsViewModel.sendingCommentByPost.collectAsState()
    val deletingPostIds by postsViewModel.deletingPostIds.collectAsState()
    val error by postsViewModel.error.collectAsState()
    val currentUserId by postsViewModel.currentUserId.collectAsState()
    val userRole by postsViewModel.userRole.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var compressingImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            compressingImage = true
            val previous = selectedImageUri
            val compressed = compressImageForPost(context, uri)
            compressingImage = false

            if (compressed != null) {
                selectedImageUri = compressed
                deleteIfTempFile(previous)
                snackbarHostState.showSnackbar("Imagen preparada para subir")
            } else {
                snackbarHostState.showSnackbar("No se pudo procesar la imagen")
            }
        }
    }

    LaunchedEffect(Unit) {
        postsViewModel.message.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error.orEmpty())
            postsViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Crear post")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loadingPosts && posts.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!loadingPosts && posts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "Todavía no hay publicaciones. ¡Sé el primero en publicar!",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    comments = commentsByPost[post.id].orEmpty(),
                    currentUserId = currentUserId,
                    sendingComment = sendingCommentByPost[post.id] == true,
                    deletingPost = deletingPostIds.contains(post.id),
                    onTogglePostLike = { postsViewModel.togglePostLike(post.id) },
                    onSendComment = { commentText -> postsViewModel.addComment(post.id, commentText) },
                    onToggleCommentLike = { commentId -> postsViewModel.toggleCommentLike(post.id, commentId) },
                    onDeletePost = { postsViewModel.deletePost(post) },
                    userRole = userRole,
                    onViewProfile = { userId ->
                        navController?.navigate(Screen.UserProfile.createRoute(userId))
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePostDialog(
            title = title,
            onTitleChange = { if (it.length <= 120) title = it },
            text = text,
            onTextChange = { if (it.length <= 2000) text = it },
            selectedImageUri = selectedImageUri,
            creatingPost = creatingPost,
            compressingImage = compressingImage,
            onPickImage = { imagePicker.launch("image/*") },
            onClearImage = {
                deleteIfTempFile(selectedImageUri)
                selectedImageUri = null
            },
            onDismiss = {
                if (!creatingPost) {
                    showCreateDialog = false
                    title = ""
                    text = ""
                    deleteIfTempFile(selectedImageUri)
                    selectedImageUri = null
                }
            },
            onPublish = {
                postsViewModel.createPost(
                    title = title,
                    text = text,
                    imageUri = selectedImageUri
                ) {
                    showCreateDialog = false
                    title = ""
                    text = ""
                    deleteIfTempFile(selectedImageUri)
                    selectedImageUri = null
                    scope.launch { snackbarHostState.showSnackbar("Tu publicación está visible para todos") }
                }
            }
        )
    }
}

@Composable
private fun CreatePostDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    selectedImageUri: Uri?,
    creatingPost: Boolean,
    compressingImage: Boolean,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva publicación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Título") },
                    singleLine = true,
                    enabled = !creatingPost,
                    supportingText = { Text("${title.length}/120") }
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("Texto") },
                    minLines = 3,
                    enabled = !creatingPost,
                    supportingText = { Text("${text.length}/2000") }
                )

                if (compressingImage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Procesando imagen...")
                    }
                }

                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Imagen de la publicación",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = onClearImage, enabled = !creatingPost && !compressingImage) {
                        Text("Quitar imagen")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPickImage,
                        enabled = !creatingPost && !compressingImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Añadir foto")
                    }

                    Button(
                        onClick = onPublish,
                        enabled = !creatingPost && !compressingImage && (title.isNotBlank() || text.isNotBlank()),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (creatingPost) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Publicar")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
internal fun PostCard(
    post: PostModel,
    comments: List<CommentModel>,
    currentUserId: String,
    sendingComment: Boolean,
    deletingPost: Boolean,
    onTogglePostLike: () -> Unit,
    onSendComment: (String) -> Unit,
    onToggleCommentLike: (String) -> Unit,
    onDeletePost: () -> Unit,
    userRole: String = "user",
    onViewProfile: (String) -> Unit = {}
) {
    var commentText by remember(post.id) { mutableStateOf("") }
    var showComments by remember(post.id) { mutableStateOf(false) }
    var showMenu by remember(post.id) { mutableStateOf(false) }
    var showDeleteDialog by remember(post.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.userAvatarUrl.ifBlank { "https://www.w3schools.com/howto/img_avatar.png" },
                    contentDescription = "Avatar del autor",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.size(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onViewProfile(post.userId) }
                    )
                    Text(
                        text = formatDateDayMonth(post.createdAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                // Mostrar menú si es el propietario O si es admin
                if (post.userId == currentUserId || userRole == "admin") {
                    IconButton(onClick = { showMenu = true }, enabled = !deletingPost) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones del post")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Eliminar post") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (post.title.isNotBlank()) {
                Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (post.text.isNotBlank()) {
                Text(text = post.text, fontSize = 15.sp)
            }

            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Imagen de la publicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val liked = post.likedBy.contains(currentUserId)
                    IconButton(onClick = onTogglePostLike, enabled = !deletingPost) {
                        Icon(
                            imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(text = post.likesCount.toString())
                }

                TextButton(onClick = { showComments = !showComments }, enabled = !deletingPost) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (showComments) "Ocultar (${comments.size})" else "Comentar (${comments.size})")
                }
            }

            if (deletingPost) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Eliminando post...")
                }
            }

            if (showComments && !deletingPost) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { if (it.length <= 500) commentText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Escribe un comentario") },
                        singleLine = true,
                        enabled = !sendingComment,
                        supportingText = { Text("${commentText.length}/500") }
                    )

                    IconButton(
                        onClick = {
                            val trimmed = commentText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendComment(trimmed)
                                commentText = ""
                            }
                        },
                        enabled = !sendingComment && commentText.isNotBlank()
                    ) {
                        if (sendingComment) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentario")
                        }
                    }
                }

                if (comments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                currentUserId = currentUserId,
                                onToggleLike = { onToggleCommentLike(comment.id) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Aún no hay comentarios",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar post") },
            text = { Text("Se eliminará el post y todos sus comentarios. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDeletePost()
                }) {
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

@Composable
private fun CommentItem(
    comment: CommentModel,
    currentUserId: String,
    onToggleLike: () -> Unit
) {
    val liked = comment.likedBy.contains(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.userAvatarUrl.ifBlank { "https://www.w3schools.com/howto/img_avatar.png" },
            contentDescription = "Avatar del comentario",
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.size(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = comment.username, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = comment.text, fontSize = 14.sp)
        }

        Row(
            modifier = Modifier.clickable(onClick = onToggleLike),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Me gusta comentario",
                tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = comment.likesCount.toString(), fontSize = 12.sp)
        }
    }
}

private fun formatDateDayMonth(millis: Long): String {
    if (millis <= 0L) return ""
    val formatter = SimpleDateFormat("dd MMM", Locale("es", "ES"))
    return formatter.format(Date(millis)).replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale("es", "ES")) else char.toString()
    }
}

private suspend fun compressImageForPost(context: android.content.Context, sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        val maxSize = 1280
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxSize || boundsOptions.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return@withContext null

        val outFile = File(context.cacheDir, "post_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, output)
            output.flush()
        }
        bitmap.recycle()

        Uri.fromFile(outFile)
    } catch (_: Exception) {
        null
    }
}

private fun deleteIfTempFile(uri: Uri?) {
    if (uri == null) return
    if (uri.scheme != "file") return
    runCatching {
        val file = File(uri.path.orEmpty())
        if (file.exists()) file.delete()
    }
}

