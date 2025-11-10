package com.jluqgon214.alphabot2.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Gestor de comunicación con el servidor de seguimiento de línea.
 * Puerto: 5003
 */
object LineFollowManager {
    private const val TAG = "LineFollowManager"
    private const val PORT = 5003

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var host: String? = null

    /**
     * Conecta al servidor de seguimiento de línea.
     */
    suspend fun connect(serverHost: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Conectando a servidor de seguimiento en $serverHost:$PORT")

            host = serverHost
            socket = Socket(serverHost, PORT)
            socket?.apply {
                soTimeout = 10000 // Timeout de 10 segundos (aumentado)
                keepAlive = true
                tcpNoDelay = true // Enviar datos inmediatamente
            }

            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

            Log.d(TAG, "✅ Conectado al servidor de seguimiento")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error conectando: ${e.message}")
            e.printStackTrace()
            disconnect()
            false
        }
    }

    /**
     * Desconecta del servidor.
     */
    fun disconnect() {
        try {
            writer?.close()
            reader?.close()
            socket?.close()
            Log.d(TAG, "Desconectado del servidor de seguimiento")
        } catch (e: Exception) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        } finally {
            writer = null
            reader = null
            socket = null
            host = null
        }
    }

    /**
     * Verifica si está conectado.
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false
    }

    /**
     * Envía un comando al servidor y espera la respuesta.
     */
    private suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                Log.e(TAG, "No conectado al servidor")
                return@withContext "ERROR:No conectado"
            }

            Log.d(TAG, "📤 Enviando comando: '$command'")

            // Enviar comando con salto de línea explícito
            writer?.println(command)
            writer?.flush()

            // Esperar respuesta
            val response = reader?.readLine()
            Log.d(TAG, "📥 Respuesta recibida: '$response'")

            if (response == null) {
                Log.e(TAG, "Respuesta nula del servidor")
                return@withContext "ERROR:Sin respuesta del servidor"
            }

            response
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "⏱️ Timeout esperando respuesta: ${e.message}")
            "ERROR:Timeout - El servidor no respondió a tiempo"
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando comando: ${e.message}")
            e.printStackTrace()
            "ERROR:${e.message}"
        }
    }

    /**
     * Inicia la calibración de sensores.
     */
    suspend fun calibrate(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("calibrate")

            when {
                response == null -> Result.failure(Exception("Sin respuesta del servidor"))
                response.startsWith("OK:") -> Result.success(response.substringAfter("OK:"))
                response.startsWith("ERROR:") -> Result.failure(Exception(response.substringAfter("ERROR:")))
                else -> Result.failure(Exception("Respuesta inesperada: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia el seguimiento de línea.
     */
    suspend fun start(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("start")

            when {
                response == null -> Result.failure(Exception("Sin respuesta del servidor"))
                response.startsWith("OK:") -> Result.success(response.substringAfter("OK:"))
                response.startsWith("ERROR:") -> Result.failure(Exception(response.substringAfter("ERROR:")))
                else -> Result.failure(Exception("Respuesta inesperada: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detiene el seguimiento de línea.
     */
    suspend fun stop(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("stop")

            when {
                response == null -> Result.failure(Exception("Sin respuesta del servidor"))
                response.startsWith("OK:") -> Result.success(response.substringAfter("OK:"))
                response.startsWith("ERROR:") -> Result.failure(Exception(response.substringAfter("ERROR:")))
                else -> Result.failure(Exception("Respuesta inesperada: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ajusta la velocidad del robot.
     * @param speed Velocidad entre 10 y 100
     */
    suspend fun setSpeed(speed: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val clampedSpeed = speed.coerceIn(10, 100)
            val response = sendCommand("speed:$clampedSpeed")

            when {
                response == null -> Result.failure(Exception("Sin respuesta del servidor"))
                response.startsWith("OK:") -> Result.success(response.substringAfter("OK:"))
                response.startsWith("ERROR:") -> Result.failure(Exception(response.substringAfter("ERROR:")))
                else -> Result.failure(Exception("Respuesta inesperada: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el estado actual del seguimiento.
     */
    suspend fun getStatus(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("status")

            if (response?.startsWith("OK:") == true) {
                Result.success(response.substringAfter("OK:"))
            } else {
                Result.failure(Exception(response?.substringAfter("ERROR:") ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

