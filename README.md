# AlphaBot2 App (TFG)

Aplicación Android desarrollada como **Trabajo Fin de Grado** para controlar un robot **AlphaBot2-Pi** equipado con **Raspberry Pi Zero 2W**.  
La app combina dos partes:

1) **Control del robot** desde el móvil (movimiento, buzzer, LEDs, etc.).  
2) **Parte social** con usuarios y publicaciones usando **Firebase**.

> Repositorio de scripts del robot (Raspberry Pi): [`JoseManuelLuque/AlphaBot2-App-Scripts`](https://github.com/JoseManuelLuque/AlphaBot2-App-Scripts)

---

## ¿Qué hace la app?

### Control del robot
- Pantalla de **configuración de conexión** (IP/usuario/contraseña).
- **Control de movimiento** con joysticks táctiles.
- Soporte de **mando Bluetooth**.
- Vista de cámara en tiempo real
- Opción **“Forzar control táctil”** (por compatibilidad con algunos móviles que detectan un mando aunque no haya nada conectado).
- Módulos extra:
  - **Buzzer / sonidos**
  - **LEDs**
  - **Seguimiento de línea** (incluido, pero con fallos actuales)

### Parte social (Firebase)
- **Registro / inicio de sesión** (Firebase Authentication).
- **Perfil de usuario** con avatar.
- **Posts** con:
  - creación de publicaciones (con o sin imagen)
  - comentarios
  - likes

---

## Tecnologías utilizadas
- **Android:** Kotlin + Jetpack Compose
- **Arquitectura:** MVVM (ViewModels)
- **Backend social:** Firebase (Auth + Firestore + Storage)
- **Robot:** Raspberry Pi Zero 2W + scripts y comunicación desde la app

---

## Limitaciones conocidas
- **Cámara:** el sistema es funcional, pero puede fallar por:
  - limitaciones de rendimiento de la Raspberry Pi Zero 2W (latencia),
  - y problemas físicos del cable flex de la cámara (contacto inestable).
- **Seguimiento de línea:** está integrado en la app, pero actualmente **no funciona correctamente** y queda como mejora pendiente.

---

## Cómo usar (resumen)
1. Inicia sesión o crea una cuenta.
2. Entra a la pantalla de **Configuración** y escribe la IP/usuario/contraseña del robot.
3. Conéctate y usa el módulo de control (joysticks / mando / buzzer / LEDs / etc.).

---

## Créditos
- Documentación y referencias del robot/sensores: **Waveshare**.
- Joysticks táctiles: se utiliza una librería externa llamada JetStick.
- Pagina de Android Developers para la progrmación del mando bluetooh

---

## Licencia
MIT
