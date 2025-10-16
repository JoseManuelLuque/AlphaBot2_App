package com.jluqgon214.alphabot2

fun executePythonScript(scriptName: String, onOutput: (String) -> Unit) {
    val command = "python /home/pi/AlphaBot2/python/$scriptName"
    SSHManager.executeCommand(command, onOutput)
}

fun stopPythonScript(onOutput: (String) -> Unit) {
    // Send SIGINT (same as Ctrl+C) to all python processes
    val command = "pkill -2 -f python"
    SSHManager.executeCommand(command, onOutput)
}
