package com.jluqgon214.alphabot2.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

/**
 * Contenido del menú lateral (drawer) de la aplicación.
 *
 * Incluye accesos rápidos a las pantallas principales y acción de cierre de sesión.
 *
 * @param navController Controlador de navegación.
 * @param onLogout Callback que se ejecuta al cerrar sesión.
 * @param onDestinationClicked Callback para cerrar el drawer tras navegar.
 */
@Composable
fun DrawerContent(
    navController: NavController,
    onLogout: () -> Unit,
    onDestinationClicked: () -> Unit
) {
    ModalDrawerSheet {
        Text("AlphaBot2 Menu", modifier = Modifier.padding(16.dp))
        HorizontalDivider()

        // Módulo de control del robot.
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Control Robot") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Config.route)
                onDestinationClicked()
            }
        )

        // Pantallas sociales y de usuario.
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Profile.route)
                onDestinationClicked()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
            label = { Text("Posts") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Posts.route)
                onDestinationClicked()
            }
        )

        // Documentación legal y de privacidad.
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Configuración") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Settings.route)
                onDestinationClicked()
            }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text("Sobre mi") },
            selected = false,
            onClick = {
                navController.navigate(Screen.About.route)
                onDestinationClicked()
            }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            label = { Text("Terminos y condiciones") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Terms.route)
                onDestinationClicked()
            }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            label = { Text("Politica de privacidad") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Privacy.route)
                onDestinationClicked()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Acción final: cerrar sesión actual.
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            label = { Text("Cerrar Sesión") },
            selected = false,
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
                onDestinationClicked()
            }
        )
    }
}
