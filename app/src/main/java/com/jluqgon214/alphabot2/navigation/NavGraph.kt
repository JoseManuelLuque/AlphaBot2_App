package com.jluqgon214.alphabot2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.screens.AboutScreen
import com.jluqgon214.alphabot2.screens.controls.ConfigScreen
import com.jluqgon214.alphabot2.screens.MainScreenWithNav
import com.jluqgon214.alphabot2.screens.PostsScreen
import com.jluqgon214.alphabot2.screens.PrivacyPolicyScreen
import com.jluqgon214.alphabot2.screens.ProfileScreen
import com.jluqgon214.alphabot2.screens.SettingsScreen
import com.jluqgon214.alphabot2.screens.TermsScreen
import com.jluqgon214.alphabot2.screens.UserProfileScreen
import com.jluqgon214.alphabot2.screens.auth.LoginScreen
import com.jluqgon214.alphabot2.screens.auth.RegisterScreen

/**
 * NavGraph: Define todas las rutas de navegación de la app.
 *
 * Estructura de navegación:
 * 1. Autenticación (Login/Register)
 * 2. Configuración (IP del robot)
 * 3. Control (Joysticks, LEDs, etc.)
 * 4. Social (Posts, Perfiles, Ajustes)
 *
 * @param navController Controlador que gestiona la navegación
 * @param innerPadding Espacios para barras del sistema
 * @param gamepadManager Gestor de mando Bluetooth
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    gamepadManager: GamepadManager,
) {
    // NavHost define el contenedor de navegación y la ruta inicial
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route // La app comienza en login
    ) {
        // ========== PANTALLAS DE AUTENTICACIÓN ==========
        // Usuario no registrado: login o signup
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // ========== PANTALLAS DE CONFIGURACIÓN ==========
        // Introducir usuario/contraseña SSH del robot para conectar
        composable(route = Screen.Config.route) {
            ConfigScreen(
                onConnect = { host, user, password, forceTouchControl ->
                    // Al conectar, ir a la pantalla principal con los datos
                    navController.navigate(Screen.Main.createRoute(host, user, password, forceTouchControl))
                }
            )
        }

        // ========== PANTALLA PRINCIPAL DE CONTROL ==========
        // Pantalla de control con joysticks y módulos (LEDs, Buzzer, etc.)
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("host") { type = NavType.StringType },
                navArgument("user") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("forceTouchControl") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            // Extraer parámetros de la ruta
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

        // ========== PANTALLAS SOCIALES ==========
        // Perfil del usuario actual
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        // Feed de posts públicos
        composable(Screen.Posts.route) {
            PostsScreen(navController = navController)
        }
        // Ajustes (idioma, admin panel, etc.)
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(Screen.Terms.route) {
            TermsScreen()
        }
        composable(Screen.Privacy.route) {
            PrivacyPolicyScreen()
        }
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                navController = navController
            )
        }
    }
}
