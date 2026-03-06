package com.decli.doudizhu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.decli.doudizhu.engine.CardFormatter
import com.decli.doudizhu.model.Card
import java.util.Locale

class RobotAnnouncer(
    context: Context,
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    private val textToSpeech = TextToSpeech(appContext, this)
    private val pendingAnnouncements = ArrayDeque<String>()
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val localeReady = sequenceOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.CHINA,
            Locale.CHINESE,
            Locale.getDefault(),
        ).firstNotNullOfOrNull { locale ->
            val result = textToSpeech.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                locale
            } else {
                null
            }
        } != null
        ready = localeReady
        if (ready) {
            textToSpeech.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            textToSpeech.setSpeechRate(0.92f)
            textToSpeech.setPitch(0.96f)
            drainPendingAnnouncements()
        }
    }

    fun announce(cards: List<Card>) {
        if (cards.isEmpty()) return
        val message = CardFormatter.aiAnnouncement(cards)
        if (!ready) {
            synchronized(pendingAnnouncements) {
                if (pendingAnnouncements.size >= 4) {
                    pendingAnnouncements.removeFirst()
                }
                pendingAnnouncements.addLast(message)
            }
            return
        }
        speak(message, TextToSpeech.QUEUE_FLUSH, "robot_play")
    }

    fun release() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        toneGenerator.release()
    }

    private fun drainPendingAnnouncements() {
        while (true) {
            val message = synchronized(pendingAnnouncements) {
                pendingAnnouncements.removeFirstOrNull()
            } ?: return
            speak(message, TextToSpeech.QUEUE_ADD, "robot_play_queue_${System.nanoTime()}")
        }
    }

    private fun speak(message: String, queueMode: Int, utteranceId: String) {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
        textToSpeech.speak(message, queueMode, null, utteranceId)
    }
}
