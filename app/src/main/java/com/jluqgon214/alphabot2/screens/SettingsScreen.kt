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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jluqgon214.alphabot2.viewmodels.LanguageViewModel
import com.jluqgon214.alphabot2.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    languageViewModel: LanguageViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()
    val scrollState = rememberScrollState()
    val userRole by profileViewModel.role.collectAsState()
    val loading by profileViewModel.loading.collectAsState()
    val error by profileViewModel.error.collectAsState()

    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚙️ Ajustes",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Personaliza tu experiencia en AlphaBot2",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    // Sección de Idioma
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "🌍 Idioma",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                            }

                            Text(
                                text = "Elige el idioma principal de la interfaz",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedLanguage == "es",
                                    onClick = { languageViewModel.setLanguage("es") },
                                    label = { Text("🇪🇸 Español") }
                                )

                                FilterChip(
                                    selected = selectedLanguage == "en",
                                    onClick = { languageViewModel.setLanguage("en") },
                                    label = { Text("🇬🇧 English") }
                                )
                            }
                        }
                    }

                    // Sección de Admin
                    if (userRole != "admin") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "🛡️ Modo Admin",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                            }

                                Text(
                                    text = "Introduce la contraseña de admin para acceder a funciones de administración",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { showAdminDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !loading
                                ) {
                                    Text("Convertirse en Admin")
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "✅ Eres administrador",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Sección Próximamente
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "✨ Próximamente",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )

                            UpcomingOptionRow(
                                icon = Icons.Default.Palette,
                                title = "🎨 Tema oscuro / claro",
                                subtitle = "Cambiar la apariencia de toda la app"
                            )

                            UpcomingOptionRow(
                                icon = Icons.Default.Notifications,
                                title = "🔔 Notificaciones",
                                subtitle = "Alertas sobre nuevos posts y actividad"
                            )

                            UpcomingOptionRow(
                                icon = Icons.Default.Security,
                                title = "🔒 Privacidad y seguridad",
                                subtitle = "Opciones de cuenta y visibilidad"
                            )

                            UpcomingOptionRow(
                                icon = Icons.Default.Info,
                                title = "ℹ️ Acerca de",
                                subtitle = "Versión de la app y créditos"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Nota: por ahora solo cambia el estado visual del idioma. Luego se conectará con localización completa.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // Diálogo de Admin
    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminDialog = false
                adminPassword = ""
                profileViewModel.clearError()
            },
            title = { Text("Contraseña de Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        enabled = !loading
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.promoteToAdmin(adminPassword)
                    },
                    enabled = !loading && adminPassword.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminDialog = false
                    adminPassword = ""
                    profileViewModel.clearError()
                }) {
                    Text("Cancelar")
                }
            }
        )

        LaunchedEffect(userRole, error) {
            if (userRole == "admin" && error == null) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "¡Ahora eres administrador!",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                showAdminDialog = false
                adminPassword = ""
            } else if (error != null && showAdminDialog && !loading) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        error!!,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    profileViewModel.clearError()
                }
            }
        }
    }
}

@Composable
private fun UpcomingOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = false,
            onCheckedChange = null,
            enabled = false
        )
    }
}
