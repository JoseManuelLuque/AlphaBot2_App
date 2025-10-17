# Archivo para facilitar la sincronización de scripts Python a la Raspberry Pi
# Uso: Edita las variables abajo y ejecuta este script en PowerShell o CMD

# === CONFIGURACIÓN ===
# Cambia estos valores por los de tu Raspberry Pi
$RASPBERRY_IP = "192.168.1.100"  # Cambia por la IP de tu Raspberry Pi
$RASPBERRY_USER = "pi"
$RASPBERRY_PATH = "/home/pi/Android App"

# === NO EDITAR DEBAJO DE ESTA LÍNEA ===
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PYTHON_SCRIPTS = Join-Path $SCRIPT_DIR "*.py"

Write-Host "🚀 Sincronizando scripts Python a Raspberry Pi..." -ForegroundColor Cyan
Write-Host "📂 Origen: $SCRIPT_DIR" -ForegroundColor Gray
Write-Host "🎯 Destino: ${RASPBERRY_USER}@${RASPBERRY_IP}:${RASPBERRY_PATH}" -ForegroundColor Gray
Write-Host ""

# Copiar archivos usando SCP
scp $PYTHON_SCRIPTS "${RASPBERRY_USER}@${RASPBERRY_IP}:${RASPBERRY_PATH}/"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Scripts copiados exitosamente" -ForegroundColor Green
    Write-Host ""
    Write-Host "🔧 Dando permisos de ejecución..." -ForegroundColor Cyan

    # Dar permisos de ejecución
    ssh "${RASPBERRY_USER}@${RASPBERRY_IP}" "chmod +x '${RASPBERRY_PATH}'/*.py"

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Permisos asignados correctamente" -ForegroundColor Green
        Write-Host ""
        Write-Host "🎉 Sincronización completada!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Error asignando permisos" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Error copiando archivos" -ForegroundColor Red
}

Write-Host ""
Write-Host "Presiona cualquier tecla para salir..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

