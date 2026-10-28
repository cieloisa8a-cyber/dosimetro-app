package com.example.dosimetrov2

import android.app.*
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DosimetryService : Service() {

    private val CHANNEL_ID = "DosimetryServiceChannel"
    private val NOTIFICATION_ID = 1

    private lateinit var audioMeter: AudioMeter
    private val engine = DosimeterEngine()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    private val _currentDose = MutableStateFlow(0f)
    val currentDose: StateFlow<Float> = _currentDose

    private val _dbWaveform = MutableStateFlow(FloatArray(0))
    val dbWaveform: StateFlow<FloatArray> = _dbWaveform

    private val _amplitudeWaveform = MutableStateFlow(FloatArray(0))
    val amplitudeWaveform: StateFlow<FloatArray> = _amplitudeWaveform

    private var alert50Triggered = false
    private var alert100Triggered = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    inner class LocalBinder : Binder() {
        fun getService(): DosimetryService = this@DosimetryService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        audioMeter = AudioMeter { db ->

            _currentDb.value = db

            // 🔹 Actualizar dosis correctamente
            engine.updateDose(db)

            val dose = engine.getDose()
            _currentDose.value = dose

            checkAlerts(dose)

            updateNotification(db, dose)
        }

        // 🔹 Escuchar historial de dB y amplitud
        serviceScope.launch {
            audioMeter.dbHistoryFlow.collect { history ->
                _dbWaveform.value = history
            }
        }
        serviceScope.launch {
            audioMeter.waveformFlow.collect { wave ->
                _amplitudeWaveform.value = wave
            }
        }
    }

    fun updateCriterion(isNiosh: Boolean) {
        if (isNiosh) {
            engine.criterionLevel = 85f
            engine.exchangeRate = 3f
            engine.thresholdLevel = 80f
        } else {
            engine.criterionLevel = 90f
            engine.exchangeRate = 5f
            engine.thresholdLevel = 80f
        }
    }

    fun getSamples() = engine.sessionSamples.toList()

    fun clearData() {
        engine.reset()
        _currentDose.value = 0f
        alert50Triggered = false
        alert100Triggered = false
    }

    private fun checkAlerts(dose: Float) {

        if (dose >= 50f && !alert50Triggered) {
            alert50Triggered = true
            // 🔹 aquí podrías cambiar color UI si quieres
        }

        if (dose >= 100f && !alert100Triggered) {
            alert100Triggered = true
            triggerVibrationAndSound()
        }
    }

    private fun triggerVibrationAndSound() {

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _isMonitoring.value = true
        val prefs = getSharedPreferences("dosimetro_prefs", MODE_PRIVATE)
        val offset = prefs.getFloat("calibration_offset", 0f)
        audioMeter.setCalibration(offset)

        val isNiosh = intent?.getBooleanExtra("IS_NIOSH", true) ?: true
        updateCriterion(isNiosh)

        val notification = createNotification(0f, 0f)
        startForeground(NOTIFICATION_ID, notification)

        audioMeter.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dosímetro Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(db: Float, dose: Float): Notification {

        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoreo en Tiempo Real")
            .setContentText(
                "Nivel: ${"%.1f".format(db)} dB(A) | Dosis: ${"%.2f".format(dose)}%"
            )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(db: Float, dose: Float) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(db, dose))
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        audioMeter.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        _isMonitoring.value = false
        audioMeter.stop()
        serviceScope.cancel() // 🔥 importante
        super.onDestroy()
    }
}