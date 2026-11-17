package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class HorrorAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    var enabled: Boolean = true

    fun playFlashlightClick(turnOn: Boolean) {
        if (!enabled) return
        val freq = if (turnOn) 1150.0 else 780.0
        playTone(durationMs = 35, freqStart = freq, freqEnd = freq * 0.55, volume = 0.25f, noiseMix = 0.45f)
    }

    fun playFootstep() {
        if (!enabled) return
        playTone(durationMs = 55, freqStart = 125.0, freqEnd = 52.0, volume = 0.14f, noiseMix = 0.35f)
    }

    fun playPartPickup() {
        if (!enabled) return
        scope.launch {
            playToneSync(durationMs = 95, freqStart = 520.0, freqEnd = 780.0, volume = 0.28f, noiseMix = 0.05f)
            playToneSync(durationMs = 130, freqStart = 880.0, freqEnd = 1040.0, volume = 0.26f, noiseMix = 0.02f)
        }
    }

    fun playBatteryPickup() {
        if (!enabled) return
        playTone(durationMs = 110, freqStart = 640.0, freqEnd = 1240.0, volume = 0.22f, noiseMix = 0.05f)
    }

    fun playHeartbeat(intensity: Float) {
        if (!enabled) return
        val vol = (0.12f + intensity * 0.28f).coerceIn(0.1f, 0.4f)
        scope.launch {
            playToneSync(durationMs = 75, freqStart = 72.0, freqEnd = 42.0, volume = vol, noiseMix = 0.0f)
            kotlinx.coroutines.delay(95)
            playToneSync(durationMs = 65, freqStart = 58.0, freqEnd = 36.0, volume = vol * 0.72f, noiseMix = 0.0f)
        }
    }

    fun playStalkerStun() {
        if (!enabled) return
        playTone(durationMs = 240, freqStart = 920.0, freqEnd = 240.0, volume = 0.32f, noiseMix = 0.45f)
    }

    fun playRepairCalibration(success: Boolean) {
        if (!enabled) return
        if (success) {
            playTone(durationMs = 120, freqStart = 440.0, freqEnd = 880.0, volume = 0.28f, noiseMix = 0.08f)
        } else {
            playTone(durationMs = 180, freqStart = 175.0, freqEnd = 110.0, volume = 0.30f, noiseMix = 0.30f)
        }
    }

    fun playJumpscare() {
        if (!enabled) return
        playTone(durationMs = 520, freqStart = 420.0, freqEnd = 135.0, volume = 0.48f, noiseMix = 0.72f)
    }

    fun playVictoryChime() {
        if (!enabled) return
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.5)
            for (n in notes) {
                playToneSync(durationMs = 135, freqStart = n, freqEnd = n * 1.01, volume = 0.24f, noiseMix = 0.0f)
            }
        }
    }

    private fun playTone(
        durationMs: Int,
        freqStart: Double,
        freqEnd: Double,
        volume: Float,
        noiseMix: Float
    ) {
        scope.launch {
            playToneSync(durationMs, freqStart, freqEnd, volume, noiseMix)
        }
    }

    private fun playToneSync(
        durationMs: Int,
        freqStart: Double,
        freqEnd: Double,
        volume: Float,
        noiseMix: Float
    ) {
        runCatching {
            val sampleRate = 22050
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return
            val samples = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toFloat() / count.toFloat()
                val freq = freqStart + (freqEnd - freqStart) * t
                phase += 2.0 * PI * freq / sampleRate
                val env = exp((-4.2f * t).toDouble()).toFloat() * (1f - t * 0.2f)
                val tone = sin(phase).toFloat()
                val noise = (Random.nextFloat() * 2f - 1f)
                val mixed = (tone * (1f - noiseMix) + noise * noiseMix) * env * volume
                samples[i] = (mixed.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 30L)
            track.stop()
            track.release()
        }
    }
}
