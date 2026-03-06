package com.decli.doudizhu.audio

import android.content.Context
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
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale.SIMPLIFIED_CHINESE
            textToSpeech.setSpeechRate(0.92f)
            textToSpeech.setPitch(0.96f)
        }
    }

    fun announce(cards: List<Card>) {
        if (!ready || cards.isEmpty()) return
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
        textToSpeech.speak(CardFormatter.aiAnnouncement(cards), TextToSpeech.QUEUE_FLUSH, null, "robot_play")
    }

    fun release() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        toneGenerator.release()
    }
}

