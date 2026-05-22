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

class MainActivity : ComponentActivity() {
    // Gestor de gamepad compartido
    val gamepadManager = GamepadManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphaBot2Theme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val showDrawer = remember { mutableStateOf(false) }

                // Observar cambios de ruta para mostrar/ocultar drawer
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        val route = backStackEntry.destination.route
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
