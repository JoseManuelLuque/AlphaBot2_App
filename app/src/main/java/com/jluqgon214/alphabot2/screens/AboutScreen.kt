package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jluqgon214.alphabot2.R
import com.jluqgon214.alphabot2.navigation.Screen

/**
 * Pantalla "Sobre mí" del desarrollador.
 *
 * Muestra:
 * - Logos del proyecto (mi_logo) e instituto (alberti)
 * - Nombre: José Manuel Luque González
 * - Email de contacto
 * - Descripción del TFG (Trabajo Fin de Grado)
 * - Enlaces a Términos y Privacidad
 *
 * @param navController NavController para navegar a otras pantallas.
 */
@Composable
fun AboutScreen(navController: NavController) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sobre mí",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Logos lado a lado: proyecto e instituto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo del proyecto
            Image(
                painter = painterResource(id = R.drawable.mi_logo),
                contentDescription = "Logo de AlphaBot2",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // Logo del instituto
            Image(
                painter = painterResource(id = R.drawable.alberti),
                contentDescription = "Logo del instituto",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "AlphaBot2 · I.E.S. Rafael Alberti",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nombre", fontWeight = FontWeight.SemiBold)
                Text("José Manuel Luque González", fontSize = 15.sp)

                Spacer(modifier = Modifier.height(4.dp))

                Text("Contacto", fontWeight = FontWeight.SemiBold)
                Text("josemanuelluquegonzalez@gmail.com", fontSize = 15.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Proyecto TFG", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "AlphaBot2 es mi Trabajo Fin de Grado. La app conecta un movil Android " +
                        "con un robot para controlarlo en tiempo real, gestionar camara, publicar posts " +
                        "y practicar funcionalidades de administracion de usuarios.",
                    fontSize = 14.sp
                )
                Text(
                    text = "La idea principal es juntar robotica, programacion movil y servicios en la nube " +
                        "en un proyecto real y util.",
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = { navController.navigate(Screen.Terms.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver terminos y condiciones")
        }

        Button(
            onClick = { navController.navigate(Screen.Privacy.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver politica de privacidad")
        }

        Text(
            text = "Gracias por usar AlphaBot2.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}


