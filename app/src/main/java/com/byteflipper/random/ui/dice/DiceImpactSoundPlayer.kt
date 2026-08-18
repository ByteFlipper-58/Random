package com.byteflipper.random.ui.dice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.provider.Settings
import com.byteflipper.random.domain.dice.physics.DiceImpactMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Small procedural impact sounds, prepared off the UI thread into static [AudioTrack] voices. */
internal class DiceImpactSoundPlayer(private val context: Context) {

    private val lock = Any()
    @Volatile
    private var voices: Map<DiceImpactMaterial, List<AudioTrack>>? = null
    private var preparing = false
    private var released = false
    private val nextVoice = IntArray(DiceImpactMaterial.entries.size)

    suspend fun prepare() = withContext(Dispatchers.Default) {
        val shouldPrepare = synchronized(lock) {
            if (released || preparing || voices != null) {
                false
            } else {
                preparing = true
                true
            }
        }
        if (!shouldPrepare) return@withContext

        val prepared = DiceImpactMaterial.entries.associateWith { material ->
            List(VOICES_PER_MATERIAL) { variant ->
                createTrack(createSample(material, variant))
            }.filterNotNull()
        }
        val releasePrepared = synchronized(lock) {
            preparing = false
            if (released) {
                true
            } else {
                voices = prepared
                false
            }
        }
        if (releasePrepared) releaseTracks(prepared)
    }

    fun play(material: DiceImpactMaterial, strength: Float) {
        if (!systemSoundsEnabled()) return
        val pool = voices?.get(material).orEmpty()
        if (pool.isEmpty()) return
        val slot = nextVoice[material.ordinal] % pool.size
        nextVoice[material.ordinal]++
        val track = pool[slot]
        try {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) track.stop()
            track.setPlaybackHeadPosition(0)
            val volume = (MIN_VOLUME + sqrt(strength.coerceIn(0f, 1f)) * VOLUME_RANGE)
                .coerceIn(0f, AudioTrack.getMaxVolume())
            track.setVolume(volume)
            track.play()
        } catch (_: IllegalStateException) {
            // Audio routing can disappear between two frames; the next impact may use another voice.
        }
    }

    fun release() {
        val prepared = synchronized(lock) {
            if (released) return
            released = true
            voices.orEmpty().also { voices = emptyMap() }
        }
        releaseTracks(prepared)
    }

    private fun releaseTracks(prepared: Map<DiceImpactMaterial, List<AudioTrack>>) {
        prepared.values.flatten().forEach { track ->
            try {
                track.release()
            } catch (_: IllegalStateException) {
                Unit
            }
        }
    }

    private fun systemSoundsEnabled(): Boolean = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED,
            1
        ) != 0
    }.getOrDefault(true)

    private fun createTrack(samples: ShortArray): AudioTrack? = runCatching {
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
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
            .build()
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track
    }.getOrNull()

    /**
     * One knock, synthesised: two decaying sine partials for the body of the sound and a short burst
     * of noise for the moment of contact.
     *
     * [variant] only detunes it. Three slightly different knocks per material are enough that a pile
     * of dice landing together does not sound like one sample retriggered, which is what gives cheap
     * dice apps their machine-gun rattle.
     */
    private fun createSample(material: DiceImpactMaterial, variant: Int): ShortArray {
        val length = (SAMPLE_RATE * SAMPLE_SECONDS).toInt()
        val output = ShortArray(length)
        val baseFrequency = when (material) {
            DiceImpactMaterial.FELT -> 145f + variant * 11f
            DiceImpactMaterial.RIM -> 285f + variant * 23f
            DiceImpactMaterial.DICE -> 405f + variant * 29f
        }
        val decay = when (material) {
            DiceImpactMaterial.FELT -> 44f
            DiceImpactMaterial.RIM -> 27f
            DiceImpactMaterial.DICE -> 36f
        }
        val noiseAmount = when (material) {
            DiceImpactMaterial.FELT -> 0.42f
            DiceImpactMaterial.RIM -> 0.13f
            DiceImpactMaterial.DICE -> 0.20f
        }
        var noiseState = 0x13579B + material.ordinal * 7919 + variant * 104729

        for (index in output.indices) {
            val time = index.toFloat() / SAMPLE_RATE
            noiseState = noiseState * 1103515245 + 12345
            val noise = (((noiseState ushr 16) and 0x7fff) / 16383.5f - 1f)
            val envelope = exp(-decay * time)
            val body = sin(2.0 * PI * baseFrequency * time).toFloat() * 0.62f +
                sin(2.0 * PI * baseFrequency * 1.83f * time).toFloat() * 0.22f
            val transient = noise * exp(-105f * time) * noiseAmount
            val value = ((body * (1f - noiseAmount * 0.35f) + transient) * envelope)
                .coerceIn(-1f, 1f)
            output[index] = (value * Short.MAX_VALUE * SAMPLE_GAIN).toInt().toShort()
        }
        return output
    }

    private companion object {
        /** Well above what a 400 Hz knock needs, and a quarter of the memory of 44.1 kHz. */
        const val SAMPLE_RATE = 22_050

        /** A die hitting felt is over before it registers as a note. */
        const val SAMPLE_SECONDS = 0.095f

        /** Headroom, so the loudest knock at full volume still cannot clip. */
        const val SAMPLE_GAIN = 0.72f

        /** Detuned copies, so simultaneous landings do not sound like one sample retriggered. */
        const val VOICES_PER_MATERIAL = 3

        /** The quietest audible nudge, and how much louder the hardest knock gets. */
        const val MIN_VOLUME = 0.10f
        const val VOLUME_RANGE = 0.44f
    }
}
