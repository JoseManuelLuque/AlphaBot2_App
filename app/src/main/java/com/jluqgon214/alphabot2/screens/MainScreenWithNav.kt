package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.navigation.BottomNavScreen
import com.jluqgon214.alphabot2.navigation.BottomNavigationBar
import com.jluqgon214.alphabot2.screens.controls.BuzzerScreen
import com.jluqgon214.alphabot2.screens.controls.LedsScreen
import com.jluqgon214.alphabot2.screens.controls.LineFollowScreen
import com.jluqgon214.alphabot2.screens.controls.MainScreen

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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
