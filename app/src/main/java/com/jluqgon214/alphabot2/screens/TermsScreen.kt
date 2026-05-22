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
 * Pantalla de Términos y Condiciones.
 *
 * Muestra la versión en español (traducida) de los términos y condiciones
 * de uso de la app AlphaBot2.
 *
 * Contiene información sobre:
 * - Licencia de uso
 * - Limitaciones de responsabilidad
 * - Derechos del usuario
 * - Políticas de actualización
 */
@Composable
fun TermsScreen() {
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
            text = "Terminos y condiciones",
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
                    Estos terminos y condiciones se aplican a la app AlphaBot2 (en adelante, la "Aplicacion") para dispositivos moviles, creada por Chemi_dev (en adelante, el "Proveedor del Servicio") como servicio de codigo abierto.
                    
                    Al descargar o usar la Aplicacion, aceptas automaticamente estos terminos. Se recomienda leerlos y entenderlos antes de usar la Aplicacion.
                    
                    El Proveedor del Servicio trabaja para que la Aplicacion sea util y eficiente. Por eso, se reserva el derecho a modificar la Aplicacion o cobrar por algunos servicios en cualquier momento y por cualquier motivo. Si hubiera algun coste, se comunicara claramente.
                    
                    La Aplicacion almacena y procesa datos personales que hayas facilitado para poder prestar el servicio. Es tu responsabilidad mantener seguro tu telefono y el acceso a la Aplicacion. El Proveedor del Servicio recomienda no hacer root/jailbreak, ya que esto elimina restricciones del sistema, puede exponer el dispositivo a malware o virus, reducir la seguridad y hacer que la Aplicacion no funcione correctamente.
                    
                    La Aplicacion usa servicios de terceros con sus propios terminos y condiciones:
                    
                    - Google Analytics for Firebase: https://www.google.com/analytics/terms/
                    - Firebase Crashlytics: https://firebase.google.com/terms/crashlytics
                    
                    El Proveedor del Servicio no asume responsabilidad por ciertos aspectos. Algunas funciones requieren conexion a internet (Wi-Fi o red movil). Si la Aplicacion no funciona al 100% por falta de conexion o por consumo de datos, el Proveedor del Servicio no se hace responsable.
                    
                    Si usas la Aplicacion fuera de una zona Wi-Fi, siguen aplicando las condiciones de tu operadora movil. Podrias tener cargos por uso de datos, roaming u otros costes de terceros. Al usar la Aplicacion, aceptas esos posibles costes. Si no eres quien paga la factura del dispositivo, se entiende que tienes permiso de la persona titular.
                    
                    Tambien es tu responsabilidad mantener el dispositivo con bateria. Si el telefono se queda sin bateria y no puedes acceder al servicio, el Proveedor del Servicio no sera responsable.
                    
                    Respecto a la exactitud y actualizacion de la informacion, el Proveedor del Servicio intenta mantener todo al dia, pero depende de informacion de terceros. No se acepta responsabilidad por perdidas directas o indirectas derivadas de confiar totalmente en esta funcionalidad.
                    
                    El Proveedor del Servicio puede actualizar la Aplicacion cuando lo considere. Los requisitos del sistema operativo pueden cambiar y tendras que instalar actualizaciones para seguir usando la Aplicacion. No se garantiza que siempre exista una actualizacion compatible con todos los dispositivos o versiones del sistema. Tambien puede dejar de ofrecer la Aplicacion en cualquier momento y sin aviso previo. En ese caso, terminaran los derechos de uso y deberas dejar de usarla y, si procede, eliminarla del dispositivo.
                    
                    Cambios en estos terminos y condiciones:
                    El Proveedor del Servicio puede actualizarlos periodicamente. Se recomienda revisar esta seccion con frecuencia. Cualquier cambio se publicara aqui.
                    
                    Fecha de entrada en vigor: 2026-05-20.
                    
                    Contacto:
                    Si tienes dudas o sugerencias sobre estos terminos y condiciones, puedes escribir a:
                    josemanuelluquegonzalez@gmail.com
                """.trimIndent()
            )
        }
    }
}

