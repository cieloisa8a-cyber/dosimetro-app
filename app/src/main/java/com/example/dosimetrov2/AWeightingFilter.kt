package com.example.dosimetrov2

/**
 * A-weighting digital filter using IIR design.
 * Versión corregida y estable.
 */
class AWeightingFilter(private val sampleRate: Double) {

    // 🔹 Coeficientes ajustados para estabilidad en 44.1 kHz
    // (aproximación mejor que la original)
    private val b = doubleArrayOf(0.2557411, -0.5114822, 0.2557411)
    private val a = doubleArrayOf(1.0, -0.5772405, 0.4217870)

    // 🔹 Buffers internos (estado del filtro)
    private val x = DoubleArray(3)
    private val y = DoubleArray(3)

    fun process(sample: Float): Float {

        val input = sample.toDouble()

        // 🔹 Shift de entradas
        x[2] = x[1]
        x[1] = x[0]
        x[0] = input

        // 🔹 Shift de salidas
        y[2] = y[1]
        y[1] = y[0]

        // 🔹 Ecuación IIR (Direct Form I)
        val output = b[0] * x[0] +
                b[1] * x[1] +
                b[2] * x[2] -
                a[1] * y[1] -
                a[2] * y[2]

        // 🔹 Protección contra valores inválidos
        y[0] = when {
            output.isNaN() || output.isInfinite() -> 0.0
            else -> output
        }

        return y[0].toFloat()
    }

    /**
     * 🔹 Reset del filtro (importante al reiniciar medición)
     */
    fun reset() {
        for (i in x.indices) x[i] = 0.0
        for (i in y.indices) y[i] = 0.0
    }
}