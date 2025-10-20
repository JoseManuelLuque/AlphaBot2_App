package com.jluqgon214.alphabot2

/**
 * Modos de velocidad para el control del AlphaBot2
 *
 * Cada modo define un multiplicador que limita la velocidad máxima:
 * - SLOW: 30% de potencia máxima
 * - STANDARD: 50% de potencia máxima
 * - FAST: 100% de potencia máxima
 */
enum class SpeedMode(val multiplier: Float, val displayName: String, val icon: String) {
    SLOW(0.3f, "Lento", "🐢"),
    STANDARD(0.5f, "Estándar", "🚶"),
    FAST(1.0f, "Rápido", "🚀");

    companion object {
        /**
         * Calcula el modo de velocidad basado en los gatillos L2/R2 del gamepad
         *
         * @param l2 Valor del gatillo L2 (0.0 a 1.0)
         * @param r2 Valor del gatillo R2 (0.0 a 1.0)
         * @return Multiplicador de velocidad calculado
         */
        fun fromTriggers(l2: Float, r2: Float): Float {
            // Si R2 está presionado, aumenta la velocidad
            if (r2 > 0.1f) {
                // Interpolar entre STANDARD (0.5) y FAST (1.0)
                return 0.5f + (r2 * 0.5f)
            }

            // Si L2 está presionado, reduce la velocidad
            if (l2 > 0.1f) {
                // Interpolar entre STANDARD (0.5) y SLOW (0.3)
                return 0.5f - (l2 * 0.2f)
            }

            // Sin gatillos = velocidad estándar
            return STANDARD.multiplier
        }
    }
}
