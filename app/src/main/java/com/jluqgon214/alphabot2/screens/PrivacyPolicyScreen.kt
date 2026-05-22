package com.jluqgon214.alphabot2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de Política de Privacidad.
 *
 * Muestra la versión en español (traducida) de la política de privacidad
 * de la app AlphaBot2.
 *
 * Contiene información sobre:
 * - Recopilación y uso de datos
 * - Acceso de terceros
 * - Derechos del usuario (opt-out, eliminación de datos)
 * - Protección de menores
 * - Medidas de seguridad
 */
@Composable
fun PrivacyPolicyScreen() {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Politica de privacidad",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Version en espanol traducida del documento original del repositorio.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                text = """
                    Esta politica de privacidad se aplica a la app AlphaBot2 (en adelante, la "Aplicacion"), creada por Chemi_dev (en adelante, el "Proveedor del Servicio") como servicio de codigo abierto. Este servicio se ofrece "tal cual".

                    1) Recogida y uso de informacion
                    La Aplicacion recopila informacion cuando la descargas y la usas. Esta informacion puede incluir:
                    - Direccion IP del dispositivo.
                    - Paginas o pantallas visitadas dentro de la app, fecha y hora de visita, y tiempo de uso.
                    - Tiempo total de uso de la aplicacion.
                    - Sistema operativo del dispositivo movil.

                    La Aplicacion no recopila informacion precisa de localizacion del dispositivo.
                    La Aplicacion no usa tecnologias de Inteligencia Artificial para procesar tus datos ni para ofrecer funciones.

                    El Proveedor del Servicio puede usar la informacion facilitada para contactar contigo puntualmente con avisos importantes, comunicaciones necesarias o promociones.

                    Para mejorar la experiencia, puede solicitar ciertos datos identificativos, incluyendo por ejemplo: josemanuelluquegonzalez@gmail.com, Male, 22, Studient, I.E.S. Rafael Alberti. Estos datos se conservaran y usaran segun esta politica.

                    2) Acceso de terceros
                    Solo se transmite periodicamente informacion agregada y anonima a servicios externos para mejorar la aplicacion.
                    La Aplicacion utiliza servicios de terceros con su propia politica de privacidad:
                    - Google Analytics for Firebase: https://firebase.google.com/support/privacy
                    - Firebase Crashlytics: https://firebase.google.com/support/privacy/

                    El Proveedor del Servicio puede divulgar informacion cuando sea necesario por ley, para proteger derechos y seguridad, para investigar fraude o para responder a solicitudes gubernamentales. Tambien puede compartir datos con proveedores de confianza que trabajan en su nombre bajo las reglas de esta politica.

                    3) Derecho de exclusion (Opt-Out)
                    Puedes detener la recopilacion de informacion desinstalando la Aplicacion mediante los procesos habituales del dispositivo o de la tienda de aplicaciones.

                    4) Conservacion de datos
                    El Proveedor del Servicio conservara los datos mientras uses la Aplicacion y durante un tiempo razonable posterior. Si deseas eliminar tus datos, escribe a josemanuelluquegonzalez@gmail.com y se atendera tu solicitud en un plazo razonable.

                    5) Menores de edad
                    La Aplicacion no esta dirigida a menores de 13 anos.
                    El Proveedor del Servicio no recopila de forma consciente datos personales de menores de 13 anos. Si se detecta ese caso, se eliminaran inmediatamente. Si eres padre, madre o tutor y sabes que un menor nos ha facilitado datos, contacta por email para tomar las medidas necesarias.

                    6) Seguridad
                    El Proveedor del Servicio aplica medidas fisicas, electronicas y de procedimiento para proteger la confidencialidad de la informacion tratada.

                    7) Cambios en esta politica
                    Esta politica puede actualizarse por cualquier motivo. Los cambios se notificaran publicando la nueva version en esta misma pagina. Se recomienda revisarla periodicamente.

                    Fecha de entrada en vigor: 2026-05-20.

                    8) Tu consentimiento
                    Al usar la Aplicacion, aceptas el tratamiento de tu informacion segun esta politica de privacidad y sus posibles actualizaciones.

                    9) Contacto
                    Si tienes preguntas sobre privacidad o sobre las practicas de la app, puedes escribir a:
                    josemanuelluquegonzalez@gmail.com
                """.trimIndent()
            )
        }
    }
}

