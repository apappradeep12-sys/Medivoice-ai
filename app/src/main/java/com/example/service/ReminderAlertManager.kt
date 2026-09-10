package com.example.service

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class ReminderAlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("ReminderAlertManager", "TTS initialization error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            textToSpeech?.setPitch(1.0f)
            textToSpeech?.setSpeechRate(0.95f)
        }
    }

    /**
     * Plays a crisp medical alert tone and vibrates
     */
    fun playReminderSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
        } catch (e: Exception) {
            // Fallback to notification ringtone
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Speaks the medicine alert aloud
     */
    fun speakVoiceAlert(medicineName: String, dosage: String, instructions: String) {
        val speech = "Attention! It is time for your medicine: $medicineName. Dosage is $dosage. $instructions."
        speak(speech)
    }

    /**
     * Speaks arbitrary text (for voice assistant responses)
     */
    fun speak(text: String) {
        if (isTtsReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MEDIVOICE_ALERT_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (ignored: Exception) {}
    }
}
