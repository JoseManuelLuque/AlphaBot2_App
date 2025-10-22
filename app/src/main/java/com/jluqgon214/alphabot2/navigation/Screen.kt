package com.jluqgon214.alphabot2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Config : Screen("config")
    object Main : Screen("main/{host}/{user}/{password}") {
        fun createRoute(host: String, user: String, password: String): String {
            return "main/$host/$user/$password"
        }
    }
}

// Pantallas con Bottom Navigation
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
        fun getAllScreens() = listOf(Control, Buzzer, Leds, LineFollow)
    }
}

