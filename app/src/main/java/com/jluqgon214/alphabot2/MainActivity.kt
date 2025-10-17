package com.jluqgon214.alphabot2

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jluqgon214.alphabot2.Screens.MainScreen
import com.jluqgon214.alphabot2.ui.theme.AlphaBot2Theme

class MainActivity : ComponentActivity() {
    // Gestor de gamepad compartido
    val gamepadManager = GamepadManager()

    override fun onCreate(savedInstanceState: Bundle?) {

        val host = "10.42.0.115"
        val user = "pi"
        val password = "raspberry"

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphaBot2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(host, user, password, innerPadding, gamepadManager)
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
