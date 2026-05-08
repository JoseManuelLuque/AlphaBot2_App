package com.jluqgon214.alphabot2.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DrawerContent(
    navController: NavController,
    onLogout: () -> Unit,
    onDestinationClicked: () -> Unit
) {
    ModalDrawerSheet {
        Text("AlphaBot2 Menu", modifier = Modifier.padding(16.dp))
        HorizontalDivider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Control Robot") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Config.route)
                onDestinationClicked()
            }
        )

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

        Spacer(modifier = Modifier.weight(1f))
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
