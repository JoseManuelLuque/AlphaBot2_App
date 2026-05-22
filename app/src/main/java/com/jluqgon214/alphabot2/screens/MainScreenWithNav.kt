package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.navigation.BottomNavScreen
import com.jluqgon214.alphabot2.navigation.BottomNavigationBar
import com.jluqgon214.alphabot2.screens.controls.BuzzerScreen
import com.jluqgon214.alphabot2.screens.controls.LedsScreen
import com.jluqgon214.alphabot2.screens.controls.LineFollowScreen
import com.jluqgon214.alphabot2.screens.controls.MainScreen

/**
 * Contenedor principal del módulo de control con navegación inferior.
 *
 * Mantiene un `NavHost` interno para cambiar entre pantallas de control sin
 * perder el contexto de conexión al robot.
 *
 * @param host IP/host del robot.
 * @param user Usuario SSH.
 * @param password Contraseña SSH.
 * @param innerPadding Padding de sistema heredado del `Scaffold` superior.
 * @param gamepadManager Gestor del mando Bluetooth.
 * @param forceTouchControl Fuerza control táctil aunque haya mando conectado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNav(
    host: String,
    user: String,
    password: String,
    innerPadding: PaddingValues,
    gamepadManager: GamepadManager,
    forceTouchControl: Boolean
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
                    gamepadManager = gamepadManager,
                    forceTouchControl = forceTouchControl
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
                    password = password,
                    innerPadding = innerPadding
                )
            }

            // Pantalla de Seguimiento de Línea
            composable(BottomNavScreen.LineFollow.route) {
                LineFollowScreen(
                    host = host,
                    user = user,
                    password = password,
                    innerPadding = innerPadding
                )
            }
        }
    }
}
