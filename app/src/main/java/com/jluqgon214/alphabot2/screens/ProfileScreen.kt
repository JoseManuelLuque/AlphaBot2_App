package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import android.app.Activity
import com.yalantis.ucrop.UCrop
import java.io.File
import androidx.core.content.FileProvider
import android.content.Intent
import com.jluqgon214.alphabot2.viewmodels.ProfileViewModel
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// ...existing imports...


/**
 * Pantalla de perfil.
 *
 * Explicación:
 * - Esta pantalla permite cambiar el nombre y el avatar del usuario.
 * - Para el avatar usamos un flujo en 2 pasos:
 *   1) El usuario elige una imagen con el selector del sistema (GetContent)
 *   2) Abrimos UCrop para que el usuario decida el recorte (interactivo)
 *      y generamos un archivo temporal en cache.
 *   3) Subimos ese archivo temporal a Firebase Storage y guardamos la URL en Firestore.
 *
 * Importante:
 * - El archivo temporal se borra al terminar la subida para no llenar el almacenamiento local.
 * - Los botones se deshabilitan mientras `loading == true` para evitar acciones simultáneas.
 */

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val username by profileViewModel.username.collectAsState()
    val email by profileViewModel.email.collectAsState()
    val avatarUrl by profileViewModel.avatarUrl.collectAsState()
    val loading by profileViewModel.loading.collectAsState()
    val error by profileViewModel.error.collectAsState()

    var editableUsername by remember { mutableStateOf(username) }
    val deleted by profileViewModel.deleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // =============== UCrop: recibir el resultado del recorte ===============
    // Este launcher recibe el resultado de la Activity de UCrop.
    // Si el usuario confirma el recorte, UCrop nos devuelve una Uri con la imagen recortada.
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                // Subimos a Firebase y luego borramos el archivo temporal.
                // (La subida se hace en IO para no bloquear el hilo principal)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        profileViewModel.uploadAvatarAndSave(resultUri)
                    } finally {
                        // Attempt to delete the temporary file created for crop
                        try {
                            val f = File(resultUri.path ?: "")
                            if (f.exists()) f.delete()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            // Si UCrop falla, aquí podríamos mostrar el error al usuario.
            // Por simplicidad limpiamos el estado de error previo.
            CoroutineScope(Dispatchers.Main).launch {
                profileViewModel.clearError()
            }
        }
    }

    // =============== Selector de imágenes (GetContent) ===============
    // Cuando el usuario elige una imagen, preparamos un archivo de salida (en cache)
    // y lanzamos UCrop para que el usuario decida el recorte.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Create a destination temp file for UCrop output
            val destFile = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
            // Use FileProvider to get a content:// URI for the dest file (safe on Android N+)
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)

            // UCrop intent
            val options = UCrop.Options().apply {
                setCompressionQuality(80)
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)
            }

            val uCrop = UCrop.of(uri, destUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1024, 1024)
                .withOptions(options)

            // Lanzamos la Activity de UCrop.
            // Añadimos flags de permisos para que UCrop pueda leer/escribir las Uri (content://)
            // y así evitar crashes por permisos o por exposición de file://
            val intent = uCrop.getIntent(context).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            cropLauncher.launch(intent)
        }
    }

    // Update editableUsername when loaded username changes
    LaunchedEffect(username) {
        editableUsername = username
    }

    // Navigate away if account deleted
    LaunchedEffect(deleted) {
        if (deleted) {
            // Navigate to login (replace stack)
            navController?.navigate("login") {
                popUpTo(0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Box centrado para la card principal
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar con overlay de edición: se puede pulsar para cambiar
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(avatarUrl.ifEmpty { "https://www.w3schools.com/howto/img_avatar.png" }),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !loading) { launcher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        IconButton(onClick = { if (!loading) launcher.launch("image/*") }, modifier = Modifier.align(Alignment.BottomEnd)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar avatar", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editableUsername,
                        onValueChange = { editableUsername = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = email,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Botón para guardar cambios de perfil
                    Button(onClick = { profileViewModel.updateUsername(editableUsername) }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Guardar cambios")
                    }

                    // Pequeño texto de ayuda
                    Text(text = "Pulsa el avatar para cambiar la imagen. Los cambios se guardan en tu cuenta.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())

                    if (loading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Logout + Delete account buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    // Logout simple
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    } catch (_: Exception) {
                    }
                    navController?.navigate("login") {
                        popUpTo(0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text(text = "Cerrar sesión")
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = "Eliminar cuenta", color = MaterialTheme.colorScheme.onError)
            }
        }

        if (showDeleteDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro? Se eliminará tu cuenta y todos tus datos. Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        profileViewModel.deleteAccount()
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
