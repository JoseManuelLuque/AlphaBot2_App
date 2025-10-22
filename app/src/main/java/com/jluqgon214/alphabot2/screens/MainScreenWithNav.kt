package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.navigation.BottomNavScreen
import com.jluqgon214.alphabot2.navigation.BottomNavigationBar

@Composable
fun MainScreenWithNav(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues,
    gamepadManager: GamepadManager
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Control.route,
            modifier = Modifier.padding(padding)
        ) {
            // Pantalla de Control (la original MainScreen)
            composable(BottomNavScreen.Control.route) {
                MainScreen(
                    host = host,
                    user = user,
                    password = password,
                    innerPadding = innerPadding,
                    gamepadManager = gamepadManager
                )
            }

            // Pantalla de Buzzer
            composable(BottomNavScreen.Buzzer.route) {
                BuzzerScreen(
                    host = host,
                    user = user,
                    password = password
                )
            }

            // Pantalla de LEDs
            composable(BottomNavScreen.Leds.route) {
                LedsScreen(
                    host = host,
                    user = user,
                    password = password
                )
            }

            // Pantalla de Seguimiento de Línea
            composable(BottomNavScreen.LineFollow.route) {
                LineFollowScreen(
                    host = host,
                    user = user,
                    password = password
                )
            }
        }
    }
}

