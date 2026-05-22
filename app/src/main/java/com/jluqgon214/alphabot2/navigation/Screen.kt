package com.jluqgon214.alphabot2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rutas de navegación de la app principal.
 *
 * Cada objeto representa una pantalla registrada en el `NavGraph`.
 * @param route Patrón de ruta que usa Navigation Compose.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Config : Screen("config")
    object Profile : Screen("profile")
    object Posts : Screen("posts")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Terms : Screen("terms")
    object Privacy : Screen("privacy")

    /** Ruta del perfil público de un usuario concreto. */
    object UserProfile : Screen("user_profile/{userId}") {
        /** Construye la ruta final para navegar al perfil público de un usuario. */
        fun createRoute(userId: String): String {
            return "user_profile/$userId"
        }
    }

    /** Ruta principal de control con parámetros de conexión al robot. */
    object Main : Screen("main/{host}/{user}/{password}/{forceTouchControl}") {
        /** Construye la ruta final incluyendo host, credenciales y modo de control. */
        fun createRoute(host: String, user: String, password: String, forceTouchControl: Boolean): String {
            return "main/$host/$user/$password/$forceTouchControl"
        }
    }
}

/**
 * Pantallas internas de la barra de navegación inferior (zona de control del robot).
 *
 * @param route Ruta interna del `NavHost` de control.
 * @param title Texto visible bajo el icono.
 * @param icon Icono mostrado en la barra inferior.
 */
sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Control : BottomNavScreen(
        route = "control",
        title = "Control",
        icon = Icons.Default.Gamepad
    )

    object Buzzer : BottomNavScreen(
        route = "buzzer",
        title = "Sonidos",
        icon = Icons.Default.VolumeUp
    )

    object Leds : BottomNavScreen(
        route = "leds",
        title = "LEDs",
        icon = Icons.Default.Light
    )

    object LineFollow : BottomNavScreen(
        route = "line_follow",
        title = "Seguimiento",
        icon = Icons.Default.ShowChart
    )

    companion object {
        /** Devuelve las pantallas en el orden en el que se pintan en la barra inferior. */
        fun getAllScreens() = listOf(Control, Buzzer, Leds, LineFollow)
    }
}
