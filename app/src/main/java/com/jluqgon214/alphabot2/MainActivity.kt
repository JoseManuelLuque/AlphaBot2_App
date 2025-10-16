package com.jluqgon214.alphabot2

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {

        val host = "10.42.0.115"
        val user = "pi"
        val password = "raspberry"

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphaBot2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(host, user, password, innerPadding)
                }
            }
        }
    }
}
