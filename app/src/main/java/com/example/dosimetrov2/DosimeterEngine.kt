package com.example.dosimetrov2

import kotlin.math.pow

data class MeasurementSample(
    val timestamp: Long,
    val spl: Float,
    val partialDose: Float
)

/**
 * DosimeterEngine: Corazón matemático del cálculo de dosis acumulada.
 * Basado en los principios de NIOSH y OSHA.
 */
class DosimeterEngine {
    // Parámetros técnicos configurables
    var criterionLevel = 85f // C (Nivel de criterio, ej. 85 dB para NIOSH)
    var exchangeRate = 3f   // ER (Tasa de intercambio, ej. 3 dB para NIOSH)
    var thresholdLevel = 80f // Umbral por debajo del cual el ruido no suma a la dosis

    private var accumulatedDose = 0.0 // Dosis acumulada en porcentaje (%)
    private var lastUpdateTime = 0L
    val sessionSamples = mutableListOf<MeasurementSample>()

    /**
     * Calcula el incremento de dosis para el nivel de SPL actual.
     * @param currentDb El nivel actual en dB(A).
     * @return El incremento de dosis (%) calculado.
     */
    fun updateDose(currentDb: Float): Float {
        val currentTime = System.currentTimeMillis()
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return 0f
        }

        // dt: Tiempo transcurrido en segundos
        val dtSeconds = (currentTime - lastUpdateTime) / 1000.0
        lastUpdateTime = currentTime

        val L = currentDb.coerceIn(35f, 110f)

        // Si el nivel es menor al umbral (Threshold), no contribuye a la dosis laboral.
        if (L < thresholdLevel) {
            sessionSamples.add(MeasurementSample(currentTime, L, 0f))
            return 0f
        }

        /**
         * FÓRMULA DE TIEMPO PERMITIDO (T):
         * T = 8h / 2^((L - C) / ER)
         * - NIOSH usa ER = 3 dB (Principio de igualdad de energía: el riesgo se duplica cada 3dB).
         * - OSHA usa ER = 5 dB (Criterio más permisivo basado en estudios históricos).
         */
        val T_allowed_hours = 8.0 / 2.0.pow(((L - criterionLevel) / exchangeRate).toDouble())
        val T_allowed_seconds = T_allowed_hours * 3600.0

        /**
         * FÓRMULA DE DOSIS (D):
         * D = 100 * (dt / T_permitido)
         */
        val doseIncrement = (dtSeconds / T_allowed_seconds) * 100.0
        accumulatedDose += doseIncrement

        sessionSamples.add(MeasurementSample(currentTime, L, doseIncrement.toFloat()))
        return doseIncrement.toFloat()
    }

    fun getDose(): Float = accumulatedDose.toFloat()

    fun reset() {
        accumulatedDose = 0.0
        lastUpdateTime = 0L
        sessionSamples.clear()
    }
}
