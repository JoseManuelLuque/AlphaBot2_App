package com.jluqgon214.alphabot2.network

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Gestor central de conexión SSH al robot.
 *
 * Se usa para lanzar scripts remotos y consultar estado de servicios en Raspberry Pi.
 */
object SSHManager {
    private var session: Session? = null

    /**
     * Abre una sesión SSH.
     *
     * @param host IP/host del robot.
     * @param user Usuario SSH.
     * @param pass Contraseña SSH.
     * @param onResult Callback con resultado de conexión.
     */
    fun connect(host: String, user: String, pass: String, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()
                session = jsch.getSession(user, host, 22)
                session?.setPassword(pass)
                session?.setConfig("StrictHostKeyChecking", "no")
                session?.connect()
                withContext(Dispatchers.Main) {
                    onResult(session?.isConnected == true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    /**
     * Ejecuta un comando remoto sobre la sesión activa y devuelve salida estándar/error.
     *
     * @param command Comando de shell a ejecutar en Raspberry Pi.
     * @param onOutput Callback con texto resultante.
     */
    fun executeCommand(command: String, onOutput: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (session?.isConnected == true) {
                var channelExec: com.jcraft.jsch.ChannelExec? = null
                try {
                    channelExec = session?.openChannel("exec") as? com.jcraft.jsch.ChannelExec
                    channelExec?.setCommand(command)

                    val inputStream = channelExec?.inputStream
                    val errorStream = channelExec?.errStream
                    channelExec?.connect()

                    val output = StringBuilder()
                    val outputReader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (outputReader.readLine().also { line = it } != null) {
                        output.append(line).append('\n')
                    }

                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    while (errorReader.readLine().also { line = it } != null) {
                        output.append("ERROR: ").append(line).append('\n')
                    }

                    withContext(Dispatchers.Main) {
                        val result = output.toString()
                        if (result.isNotBlank()) {
                            onOutput(result)
                        } else {
                            onOutput("Comando ejecutado sin salida.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        onOutput("Excepción: ${e.message}")
                    }
                } finally {
                    channelExec?.disconnect()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onOutput("No se puede ejecutar el comando: SSH no conectado.")
                }
            }
        }
    }

    /** Cierra la sesión SSH activa y limpia recursos. */
    fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                session?.disconnect()
                session = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

