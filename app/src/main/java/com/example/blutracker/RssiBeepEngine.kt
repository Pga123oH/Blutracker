package com.example.blutracker.rssi

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class RssiBeepEngine(context: Context) {



    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(audioAttributes)
        .build()

    private val beepSoundId: Int


    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        val pcm = generateBeepPcm(frequencyHz = 880, durationMs = 60, sampleRate = 44100)
        val wavBytes = pcmToWav(pcm, sampleRate = 44100)
        beepSoundId = soundPool.load(wavBytesToTempFile(context, wavBytes), 1)
    }



    private val txPowerAt1m: Int = -59
    private val pathLossExponent: Double = 2.5
    private val maxDistance: Double = 15.0



    private val maxIntervalMs: Long = 2000L
    private val minIntervalMs: Long = 80L



    @Volatile private var currentRssi: Int = -100
    @Volatile private var running: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val beepRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            playBeepAndVibrate() // CALLING THE NEW DUAL FUNCTION
            val interval = rssiToIntervalMs(currentRssi)
            handler.postDelayed(this, interval)
        }
    }



    fun start() {
        if (running) return
        running = true
        handler.post(beepRunnable)
    }

    fun updateRssi(rssi: Int) {
        currentRssi = rssi
    }

    fun stop() {
        running = false
        handler.removeCallbacks(beepRunnable)
        soundPool.release()
    }



    private fun playBeepAndVibrate() {
        // 1. Play the audio beep
        soundPool.play(beepSoundId, 1f, 1f, 1, 0, 1f)

        // 2. Trigger a short haptic pulse (60ms to match the audio length)
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        }
    }

    private fun rssiToIntervalMs(rssi: Int): Long {
        val exponent = (txPowerAt1m - rssi).toDouble() / (10.0 * pathLossExponent)
        val distanceM = 10.0.pow(exponent)
        val ratio = (distanceM / maxDistance).coerceIn(0.04, 1.0)
        return (minIntervalMs + ratio * (maxIntervalMs - minIntervalMs)).toLong()
    }



    private fun generateBeepPcm(
        frequencyHz: Int,
        durationMs: Int,
        sampleRate: Int
    ): ShortArray {
        val numSamples = sampleRate * durationMs / 1000
        val pcm = ShortArray(numSamples)
        val twoPiF = 2.0 * Math.PI * frequencyHz / sampleRate
        for (i in 0 until numSamples) {
            val envelope = 0.5 * (1 - kotlin.math.cos(2.0 * Math.PI * i / numSamples))
            val sample = envelope * kotlin.math.sin(twoPiF * i) * Short.MAX_VALUE
            pcm[i] = sample.toInt().toShort()
        }
        return pcm
    }

    private fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcm.size * 2
        val totalSize = 44 + dataSize
        val buf = ByteArray(totalSize)
        fun writeStr(offset: Int, s: String) = s.forEachIndexed { i, c -> buf[offset + i] = c.code.toByte() }
        fun writeInt(offset: Int, v: Int) {
            buf[offset]     = (v and 0xFF).toByte()
            buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
            buf[offset + 2] = ((v shr 16) and 0xFF).toByte()
            buf[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun writeShort(offset: Int, v: Int) {
            buf[offset]     = (v and 0xFF).toByte()
            buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }
        writeStr(0,  "RIFF")
        writeInt(4,  totalSize - 8)
        writeStr(8,  "WAVE")
        writeStr(12, "fmt ")
        writeInt(16, 16)
        writeShort(20, 1)
        writeShort(22, 1)
        writeInt(24, sampleRate)
        writeInt(28, sampleRate * 2)
        writeShort(32, 2)
        writeShort(34, 16)
        writeStr(36, "data")
        writeInt(40, dataSize)
        pcm.forEachIndexed { i, s ->
            buf[44 + i * 2]     = (s.toInt() and 0xFF).toByte()
            buf[44 + i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }
        return buf
    }

    private fun wavBytesToTempFile(context: Context, bytes: ByteArray): String {
        val file = java.io.File(context.cacheDir, "beep_tone.wav")
        file.outputStream().use { it.write(bytes) }
        return file.absolutePath
    }
}