package com.jluqgon214.alphabot2.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

object SocketManager {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var connectionJob: Job? = null
    private const val TAG = "SocketManager"
    private const val PORT = 5555

    fun connect(host: String, onResult: (Boolean) -> Unit) {
        connectionJob?.cancel()
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                disconnect()

                socket = Socket(host, PORT).apply {
                    tcpNoDelay = true
                    soTimeout = 5000
                }

                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                Log.d(TAG, "Conectado al servidor joystick en $host:$PORT")

                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun sendJoystickData(x: Float, y: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("MOVE $x $y")
                } else {
                    Log.w(TAG, "Socket no conectado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando datos: ${e.message}")
            }
        }
    }

    fun sendCameraData(x: Float, y: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("CAMERA $x $y")
                } else {
                    Log.w(TAG, "Socket no conectado para cámara")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando datos de cámara: ${e.message}")
            }
        }
    }

    fun stop() {
        sendJoystickData(0f, 0f)
    }

    fun disconnect() {
        try {
            writer?.println("quit")
            writer?.close()
            reader?.close()
            socket?.close()
            socket = null
            writer = null
            reader = null
            Log.d(TAG, "Desconectado del servidor joystick")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket!!.isClosed
    }
}

