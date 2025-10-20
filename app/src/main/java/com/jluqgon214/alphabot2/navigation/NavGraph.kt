package com.jluqgon214.alphabot2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jluqgon214.alphabot2.GamepadManager
import com.jluqgon214.alphabot2.screens.ConfigScreen
import com.jluqgon214.alphabot2.screens.MainScreen
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    gamepadManager: GamepadManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Config.route
    ) {
        // Pantalla de configuración (inicial)
        composable(route = Screen.Config.route) {
            ConfigScreen(
                onConnect = { host, user, password ->
                    navController.navigate(Screen.Main.createRoute(host, user, password))
                }
            )
        }

        // Pantalla principal de control
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("host") { type = NavType.StringType },
                navArgument("user") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val host = backStackEntry.arguments?.getString("host") ?: ""
            val user = backStackEntry.arguments?.getString("user") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""

            MainScreen(
                host = host,
                user = user,
                password = password,
                innerPadding = innerPadding,
                gamepadManager = gamepadManager
            )
        }
    }
}

