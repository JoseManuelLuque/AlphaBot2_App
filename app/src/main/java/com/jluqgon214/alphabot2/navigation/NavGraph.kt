package com.jluqgon214.alphabot2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.screens.ConfigScreen
import com.jluqgon214.alphabot2.screens.MainScreenWithNav

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
                onConnect = { host, user, password, forceTouchControl ->
                    navController.navigate(Screen.Main.createRoute(host, user, password, forceTouchControl))
                }
            )
        }

        // Pantalla principal de control con Bottom Navigation
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("host") { type = NavType.StringType },
                navArgument("user") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("forceTouchControl") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val host = backStackEntry.arguments?.getString("host") ?: ""
            val user = backStackEntry.arguments?.getString("user") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val forceTouchControl = backStackEntry.arguments?.getBoolean("forceTouchControl") ?: false

            MainScreenWithNav(
                host = host,
                user = user,
                password = password,
                innerPadding = innerPadding,
                gamepadManager = gamepadManager,
                forceTouchControl = forceTouchControl
            )
        }
    }
}
