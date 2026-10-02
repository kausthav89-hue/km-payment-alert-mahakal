package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class PaymentTTSManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "PaymentTTSManager"

        @Volatile
        private var instance: PaymentTTSManager? = null

        fun getInstance(context: Context): PaymentTTSManager {
            return instance ?: synchronized(this) {
                instance ?: PaymentTTSManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalVolume: Int? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            configureAudioAttributes()
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            isInitialized = false
            Log.e(TAG, "Failed to initialize TextToSpeech engine")
        }
    }

    private fun configureAudioAttributes() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM) // Bypasses standard silent mode
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts?.setAudioAttributes(audioAttributes)
    }

    /**
     * Speaks the payment announcement loudly.
     * Can temporarily boost alarm/music volume to max and restore original volume when done.
     */
    fun speakPayment(
        text: String,
        language: String,
        boostVolume: Boolean = true,
        speechRate: Float = 0.95f,
        onComplete: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            // Re-init if destroyed
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    configureAudioAttributes()
                    performSpeak(text, language, boostVolume, speechRate, onComplete)
                }
            }
            return
        }

        performSpeak(text, language, boostVolume, speechRate, onComplete)
    }

    private fun performSpeak(
        text: String,
        language: String,
        boostVolume: Boolean,
        speechRate: Float,
        onComplete: (() -> Unit)?
    ) {
        val targetLocale = if (language == "bn") {
            Locale.forLanguageTag("bn-IN")
        } else {
            Locale.forLanguageTag("en-IN")
        }

        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to broader locale
            if (language == "bn") {
                val fallbackBn = Locale.forLanguageTag("bn")
                val fbResult = tts?.setLanguage(fallbackBn)
                if (fbResult == TextToSpeech.LANG_MISSING_DATA || fbResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
            } else {
                tts?.language = Locale.US
            }
        }

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(1.0f)

        // Boost volume if enabled
        val streamType = AudioManager.STREAM_ALARM
        if (boostVolume) {
            try {
                if (originalVolume == null) {
                    originalVolume = audioManager.getStreamVolume(streamType)
                }
                val maxVol = audioManager.getStreamMaxVolume(streamType)
                audioManager.setStreamVolume(streamType, maxVol, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to boost stream volume: ${e.message}")
            }
        }

        val utteranceId = "payment_${UUID.randomUUID()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {
                Log.d(TAG, "TTS speech started: $uttId")
            }

            override fun onDone(uttId: String?) {
                restoreVolume()
                onComplete?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(uttId: String?) {
                restoreVolume()
                onComplete?.invoke()
            }

            override fun onError(uttId: String?, errorCode: Int) {
                restoreVolume()
                onComplete?.invoke()
            }
        })

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun restoreVolume() {
        originalVolume?.let { savedVol ->
            try {
                val streamType = AudioManager.STREAM_ALARM
                audioManager.setStreamVolume(streamType, savedVol, 0)
                originalVolume = null
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring volume: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            tts?.stop()
            restoreVolume()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            // ignore
        }
    }
}
