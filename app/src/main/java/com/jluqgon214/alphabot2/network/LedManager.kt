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

/**
 * Gestor de comunicación TCP con el servidor de LEDs del robot (puerto 5556).
 *
 * Comandos soportados: `ON`, `OFF`, `COLOR`, `BRIGHTNESS`, `EFFECT`, `QUIT`.
 */
object LedManager {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var connectionJob: Job? = null
    private const val TAG = "LedManager"
    private const val PORT = 5556 // Puerto del servidor de LEDs

    /** Conecta con el servidor remoto de LEDs. */
    fun connect(host: String, onResult: (Boolean) -> Unit) {
        connectionJob?.cancel()
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                disconnect()

                Log.d(TAG, "Intentando conectar al servidor de LEDs en $host:$PORT...")
                socket = Socket(host, PORT).apply {
                    tcpNoDelay = true
                    soTimeout = 5000
                }

                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                Log.d(TAG, "✅ Conectado al servidor de LEDs")

                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo conectar al servidor de LEDs: ${e.message?.substringBefore(":")}")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    /** Enciende los LEDs remotos. */
    fun turnOn(onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("ON")
                    val response = reader?.readLine()
                    val success = response == "OK"
                    Log.d(TAG, "💡 Encender LEDs: ${if (success) "OK" else "ERROR"}")

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(success)
                    }
                } else {
                    Log.w(TAG, "Socket no conectado")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando ON: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    /** Apaga los LEDs remotos. */
    fun turnOff(onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("OFF")
                    val response = reader?.readLine()
                    val success = response == "OK"
                    Log.d(TAG, "💡 Apagar LEDs: ${if (success) "OK" else "ERROR"}")

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(success)
                    }
                } else {
                    Log.w(TAG, "Socket no conectado")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando OFF: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    /** Establece color RGB en rango 0..255 por canal. */
    fun setColor(red: Int, green: Int, blue: Int, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("COLOR $red $green $blue")
                    val response = reader?.readLine()
                    val success = response == "OK"
                    Log.d(TAG, "🎨 Cambiar color RGB($red, $green, $blue): ${if (success) "OK" else "ERROR"}")

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(success)
                    }
                } else {
                    Log.w(TAG, "Socket no conectado")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando COLOR: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    /** Ajusta brillo global en porcentaje 0..100. */
    fun setBrightness(brightness: Int, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("BRIGHTNESS $brightness")
                    val response = reader?.readLine()
                    val success = response == "OK"
                    Log.d(TAG, "💫 Cambiar brillo a $brightness%: ${if (success) "OK" else "ERROR"}")

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(success)
                    }
                } else {
                    Log.w(TAG, "Socket no conectado")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando BRIGHTNESS: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    /** Cambia el efecto/animación activa del servidor LED. */
    fun setEffect(effect: String, onResult: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (socket?.isConnected == true && writer != null) {
                    writer?.println("EFFECT $effect")
                    val response = reader?.readLine()
                    val success = response == "OK"
                    Log.d(TAG, "✨ Cambiar efecto a $effect: ${if (success) "OK" else "ERROR"}")

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(success)
                    }
                } else {
                    Log.w(TAG, "Socket no conectado")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando EFFECT: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    /** Cierra conexión con el servidor de LEDs y limpia recursos. */
    fun disconnect() {
        try {
            writer?.println("QUIT")
            writer?.close()
            reader?.close()
            socket?.close()
            socket = null
            writer = null
            reader = null
            Log.d(TAG, "Desconectado del servidor de LEDs")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando: ${e.message}")
        }
    }

    /** Indica si la conexión TCP está activa. */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket!!.isClosed
    }
}

