package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jluqgon214.alphabot2.viewmodels.LanguageViewModel

/**
 * Pantalla de Configuración.
 *
 * Permite al usuario ajustar las preferencias de la aplicación.
 * Por ahora incluye: cambio de idioma (español/inglés).
 * Futura expansión: tema oscuro/claro, notificaciones, privacidad, etc.
 */
@Composable
fun SettingsScreen(
    languageViewModel: LanguageViewModel = viewModel()
) {
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Título
        Text(
            text = "Configuración",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Sección: Idioma
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Encabezado de la sección
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Idioma",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Idioma",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Opción: Español
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedLanguage == "es",
                        onClick = { languageViewModel.setLanguage("es") }
                    )
                    Text(
                        text = "Español",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Opción: Inglés
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedLanguage == "en",
                        onClick = { languageViewModel.setLanguage("en") }
                    )
                    Text(
                        text = "English",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Espacio futuro para más opciones
        Text(
            text = "Más opciones (próximamente):",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
        )
        Text(
            text = "• Tema oscuro/claro\n• Notificaciones\n• Privacidad y seguridad\n• Acerca de",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

