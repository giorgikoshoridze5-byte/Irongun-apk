package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.WeaponType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class TacticalAudioEngine {
    @Volatile
    var isEnabled: Boolean = true

    private val sampleRate = 22050
    private val m4Samples = synthesizeShot(durationMs = 110, baseFreq = 190f, noiseWeight = 0.68f, decay = 26f)
    private val mp9Samples = synthesizeShot(durationMs = 80, baseFreq = 260f, noiseWeight = 0.62f, decay = 36f)
    private val ax50Samples = synthesizeShot(durationMs = 230, baseFreq = 95f, noiseWeight = 0.75f, decay = 13f)
    private val m870Samples = synthesizeShot(durationMs = 190, baseFreq = 120f, noiseWeight = 0.85f, decay = 16f)
    private val p226Samples = synthesizeShot(durationMs = 88, baseFreq = 240f, noiseWeight = 0.60f, decay = 32f)
    private val deagleSamples = synthesizeShot(durationMs = 175, baseFreq = 110f, noiseWeight = 0.72f, decay = 18f)
    private val g18Samples = synthesizeShot(durationMs = 70, baseFreq = 280f, noiseWeight = 0.58f, decay = 40f)
    private val explosionSamples = synthesizeShot(durationMs = 420, baseFreq = 58f, noiseWeight = 0.90f, decay = 7.5f)
    private val hitMarkerSamples = synthesizeTone(durationMs = 42, freq = 1480f, decay = 55f, amp = 0.45f)
    private val headshotSamples = synthesizeTone(durationMs = 85, freq = 2280f, decay = 32f, amp = 0.60f)
    private val reloadSamples = synthesizeTone(durationMs = 65, freq = 620f, decay = 42f, amp = 0.35f)
    private val hackSamples = synthesizeTone(durationMs = 140, freq = 960f, decay = 16f, amp = 0.45f)

    fun playWeaponFire(weapon: WeaponType) {
        if (!isEnabled) return
        val pcm = when (weapon) {
            WeaponType.M4A1 -> m4Samples
            WeaponType.MP9 -> mp9Samples
            WeaponType.AX50 -> ax50Samples
            WeaponType.M870 -> m870Samples
            WeaponType.P226 -> p226Samples
            WeaponType.DEAGLE -> deagleSamples
            WeaponType.G18 -> g18Samples
        }
        playPcm(pcm)
    }

    fun playHitMarker(isHeadshot: Boolean) {
        if (!isEnabled) return
        playPcm(if (isHeadshot) headshotSamples else hitMarkerSamples)
    }

    fun playReload() {
        if (!isEnabled) return
        playPcm(reloadSamples)
    }

    fun playExplosion() {
        if (!isEnabled) return
        playPcm(explosionSamples)
    }

    fun playTurretHack() {
        if (!isEnabled) return
        playPcm(hackSamples)
    }

    private fun playPcm(samples: ShortArray) {
        try {
            val bufferBytes = samples.size * 2
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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.release()
                    } catch (_: Exception) {}
                }

                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Exception) {
            // Safe no-op on audio-unavailable hardware
        }
    }

    private fun synthesizeShot(
        durationMs: Int,
        baseFreq: Float,
        noiseWeight: Float,
        decay: Float
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        val rng = Random(durationMs)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-decay * t)
            val pitchDrop = baseFreq * (1f + 1.8f * exp(-45f * t))
            val thump = sin(2.0 * PI * pitchDrop * t).toFloat()
            val noise = rng.nextFloat() * 2f - 1f
            val mixed = ((1f - noiseWeight) * thump + noiseWeight * noise) * env * 0.72f
            out[i] = (mixed.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthesizeTone(
        durationMs: Int,
        freq: Float,
        decay: Float,
        amp: Float
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-decay * t)
            val s = sin(2.0 * PI * freq * t).toFloat() * env * amp
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
