package com.example.dosimetrov2

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * AudioMeter: Clase encargada de la captura de audio crudo y procesamiento digital (DSP).
 * Implementa la Ponderación A para simular la sensibilidad del oído humano.
 */
class AudioMeter(private val onUpdate: (Float) -> Unit) {

    private val sampleRate = 44100
    // Tamaño del buffer para capturar muestras de audio sin pérdida
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(8192)

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var job: Job? = null
    private var calibrationOffset = 0f

    // Flujos para actualizar la interfaz en tiempo real (Flows)
    private val _dbHistoryFlow = MutableStateFlow(FloatArray(0))
    val dbHistoryFlow: StateFlow<FloatArray> = _dbHistoryFlow

    private val _waveformFlow = MutableStateFlow(FloatArray(0))
    val waveformFlow: StateFlow<FloatArray> = _waveformFlow

    private val dbHistory = mutableListOf<Float>()
    private val ampHistory = mutableListOf<Float>()
    private val MAX_HISTORY = 100

    // Variables del Filtro de Ponderación A (IIR Direct Form II)
    // Estos coeficientes aproximan la curva ISO 226 (Curvas de igual sonoridad)
    private var z1 = 0.0
    private var z2 = 0.0
    private val b0 = 0.16999495; private val b1 = 0.28039060; private val b2 = -0.11204747
    private val a1 = -2.12979410; private val a2 = 0.42996125

    fun setCalibration(offset: Float) {
        calibrationOffset = offset
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        Log.d("AudioMeter", "Inicializando captura de audio...")

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioMeter", "Error: Hardware de audio no disponible")
                return
            }

            audioRecord?.startRecording()
            isRunning = true

            job = CoroutineScope(Dispatchers.Default).launch {
                val readBuffer = ShortArray(bufferSize)
                while (isRunning) {
                    val currentRecord = audioRecord ?: break
                    val read = currentRecord.read(readBuffer, 0, readBuffer.size)
                    
                    if (read > 0) {
                        var sumSquare = 0.0
                        var maxVal = 0

                        for (i in 0 until read) {
                            val sampleRaw = readBuffer[i].toInt()
                            if (abs(sampleRaw) > maxVal) maxVal = abs(sampleRaw)
                            
                            val x = sampleRaw.toDouble()
                            // Aplicación del filtro de ponderación A
                            val out = b0 * x + z1
                            z1 = b1 * x - a1 * out + z2
                            z2 = b2 * x - a2 * out
                            
                            // Si el filtro produce un error matemático, usamos el valor original
                            val finalSample = if (out.isNaN() || out.isInfinite()) x else out
                            sumSquare += finalSample * finalSample
                        }

                        // Cálculo del RMS (Raíz Cuadrática Media)
                        val rms = sqrt(sumSquare / read)
                        val normalizedRMS = rms / 32768.0 // Normalización a escala 0.0 - 1.0
                        
                        /**
                         * Conversión a decibelios (dB).
                         * Referencia: 100 dB SPL corresponden al máximo nivel digital (0 dBFS).
                         */
                        val db = if (normalizedRMS > 1e-10) {
                            (20.0 * log10(normalizedRMS) + 100.0 + calibrationOffset).toFloat()
                        } else {
                            35.0f
                        }

                        val finalDb = db.coerceIn(35f, 110f)
                        val currentMaxAmp = maxVal.toFloat() / 32768f

                        updateHistories(finalDb, currentMaxAmp)
                        
                        // Enviamos el valor a la UI en el hilo principal
                        withContext(Dispatchers.Main) {
                            onUpdate(finalDb)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioMeter", "Error en hilo de audio: ${e.message}")
        }
    }

    private fun updateHistories(db: Float, amp: Float) {
        dbHistory.add(db)
        if (dbHistory.size > MAX_HISTORY) dbHistory.removeAt(0)
        _dbHistoryFlow.value = dbHistory.toFloatArray()

        ampHistory.add(amp)
        if (ampHistory.size > MAX_HISTORY) ampHistory.removeAt(0)
        _waveformFlow.value = ampHistory.toFloatArray()
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioMeter", "Fallo al detener")
        }
        audioRecord = null
    }
}
