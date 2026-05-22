package com.jluqgon214.alphabot2

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jluqgon214.alphabot2.gamepad.GamepadManager
import com.jluqgon214.alphabot2.navigation.DrawerContent
import com.jluqgon214.alphabot2.navigation.NavGraph
import com.jluqgon214.alphabot2.navigation.Screen
import com.jluqgon214.alphabot2.ui.theme.AlphaBot2Theme
import kotlinx.coroutines.launch

/**
 * MainActivity: Punto de entrada principal de la aplicación.
 *
 * Responsable de:
 * - Configurar el tema visual
 * - Gestionar la navegación entre pantallas
 * - Controlar el menú lateral (drawer)
 * - Gestionar el mando Bluetooth (gamepad)
 */
class MainActivity : ComponentActivity() {
    // Objeto compartido para gestionar la conexión con mandos Bluetooth
    val gamepadManager = GamepadManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilitar edge-to-edge (usar todo el espacio de la pantalla)
        enableEdgeToEdge()
        // Configurar la interfaz usando Jetpack Compose
        setContent {
            AlphaBot2Theme {
                // Controlador de navegación
                val navController = rememberNavController()
                // Estado del menú lateral (abierto/cerrado)
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                // Estado para mostrar/ocultar el drawer según la pantalla actual
                val showDrawer = remember { mutableStateOf(false) }

                // Monitorizar cambios de pantalla para saber cuándo mostrar el drawer
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        val route = backStackEntry.destination.route
                        // No mostrar drawer en pantallas de login/registro
                        showDrawer.value = route !in listOf(Screen.Login.route, Screen.Register.route)
                    }
                }

                val drawerContentLambda = if (showDrawer.value) {
                    @Composable {
                        DrawerContent(navController = navController, onLogout = {
                            scope.launch {
                                drawerState.close()
                            }
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Config.route) { inclusive = true }
                            }
                        },
                            onDestinationClicked = {
                                scope.launch {
                                    drawerState.close()
                                }
                            })
                    }
                } else {
                    @Composable {}
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = drawerContentLambda
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            innerPadding = innerPadding,
                            gamepadManager = gamepadManager
                        )
                    }
                }
            }
        }
    }

    // ===== CAPTURA DE EVENTOS DEL MANDO =====

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Procesar movimiento de sticks y triggers
        return if (gamepadManager.onMotionEvent(event)) {
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Procesar botones presionados
        return if (gamepadManager.onKeyEvent(event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Procesar botones soltados
        return if (gamepadManager.onKeyEvent(event)) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }
}
