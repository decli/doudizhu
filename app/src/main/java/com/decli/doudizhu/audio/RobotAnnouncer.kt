package com.decli.doudizhu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.decli.doudizhu.R
import com.decli.doudizhu.engine.CardFormatter
import com.decli.doudizhu.engine.ComboType
import com.decli.doudizhu.engine.RuleEngine
import com.decli.doudizhu.model.Card
import java.util.Locale
import java.util.concurrent.Executors

class RobotAnnouncer(
    context: Context,
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val voiceExecutor = Executors.newSingleThreadExecutor()
    @Volatile
    private var initializationAttempted = false
    @Volatile
    private var ttsReady = false
    @Volatile
    private var textToSpeech: TextToSpeech? = null

    override fun onInit(status: Int) {
        val tts = textToSpeech ?: return
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = sequenceOf(
                Locale.SIMPLIFIED_CHINESE,
                Locale.CHINA,
                Locale.CHINESE,
                Locale.getDefault(),
            ).firstNotNullOfOrNull { locale ->
                val result = tts.setLanguage(locale)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    locale
                } else {
                    null
                }
            } != null
            if (ttsReady) {
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                tts.setSpeechRate(0.92f)
                tts.setPitch(0.96f)
            }
        }
    }

    fun announce(cards: List<Card>) {
        if (cards.isEmpty()) return
        ensureInitialized()
        playCue(buildCue(cards), TextToSpeech.QUEUE_FLUSH, "robot_play")
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        voiceExecutor.shutdownNow()
    }

    private fun playCue(cue: AnnouncementCue, queueMode: Int, utteranceId: String) {
        val speakResult = if (ttsReady) {
            speak(cue.message, queueMode, utteranceId)
        } else {
            TextToSpeech.ERROR
        }
        if (speakResult != TextToSpeech.SUCCESS) {
            playFallback(cue)
        }
    }

    private fun speak(message: String, queueMode: Int, utteranceId: String): Int {
        val tts = textToSpeech ?: return TextToSpeech.ERROR
        return runCatching {
            tts.speak(message, queueMode, null, utteranceId)
        }.getOrElse {
            ttsReady = false
            TextToSpeech.ERROR
        }
    }

    private fun playFallback(cue: AnnouncementCue) {
        voiceExecutor.execute {
            cue.clips.forEach { clip ->
                if (!playClip(clip.resId)) {
                    return@execute
                }
                Thread.sleep(18)
            }
        }
    }

    private fun playClip(resId: Int): Boolean {
        return runCatching {
            val mediaPlayer = MediaPlayer.create(appContext, resId) ?: return false
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            try {
                mediaPlayer.start()
                while (mediaPlayer.isPlaying) {
                    Thread.sleep(30)
                }
            } finally {
                mediaPlayer.release()
            }
            true
        }.getOrDefault(false)
    }

    private fun ensureInitialized() {
        if (initializationAttempted) return
        synchronized(this) {
            if (initializationAttempted) return
            initializationAttempted = true
            textToSpeech = runCatching {
                TextToSpeech(appContext, this)
            }.getOrNull()
        }
    }

    private fun buildCue(cards: List<Card>): AnnouncementCue {
        val message = CardFormatter.aiAnnouncement(cards)
        val combo = RuleEngine.identifyCombo(cards)
        val clips = when (combo?.type) {
            ComboType.Single -> listOf(rankClip(combo.primaryRank))
            ComboType.Pair -> listOf(RobotClip.Pair, rankClip(combo.primaryRank))
            ComboType.Triple -> listOf(RobotClip.Triple, rankClip(combo.primaryRank))
            ComboType.TripleSingle -> listOf(RobotClip.TripleSingle)
            ComboType.TriplePair -> listOf(RobotClip.TriplePair)
            ComboType.Straight -> listOf(RobotClip.Straight)
            ComboType.PairStraight -> listOf(RobotClip.PairStraight)
            ComboType.Plane -> listOf(RobotClip.Plane)
            ComboType.PlaneSingle -> listOf(RobotClip.PlaneSingle)
            ComboType.PlanePair -> listOf(RobotClip.PlanePair)
            ComboType.FourTwoSingle -> listOf(RobotClip.FourTwoSingle)
            ComboType.FourTwoPair -> listOf(RobotClip.FourTwoPair)
            ComboType.Bomb -> listOf(rankClip(combo.primaryRank), RobotClip.Bomb)
            ComboType.Rocket -> listOf(RobotClip.Rocket)
            null -> listOf(RobotClip.Play)
        }
        return AnnouncementCue(message = message, clips = clips)
    }

    private fun rankClip(rank: Int): RobotClip = when (rank) {
        3 -> RobotClip.Rank3
        4 -> RobotClip.Rank4
        5 -> RobotClip.Rank5
        6 -> RobotClip.Rank6
        7 -> RobotClip.Rank7
        8 -> RobotClip.Rank8
        9 -> RobotClip.Rank9
        10 -> RobotClip.Rank10
        11 -> RobotClip.RankJ
        12 -> RobotClip.RankQ
        13 -> RobotClip.RankK
        14 -> RobotClip.RankA
        15 -> RobotClip.Rank2
        16 -> RobotClip.SmallJoker
        17 -> RobotClip.BigJoker
        else -> RobotClip.Play
    }

    private data class AnnouncementCue(
        val message: String,
        val clips: List<RobotClip>,
    )

    private enum class RobotClip(val resId: Int) {
        Play(R.raw.play),
        Pair(R.raw.pair),
        Triple(R.raw.triple),
        TripleSingle(R.raw.triple_single),
        TriplePair(R.raw.triple_pair),
        Straight(R.raw.straight),
        PairStraight(R.raw.pair_straight),
        Plane(R.raw.plane),
        PlaneSingle(R.raw.plane_single),
        PlanePair(R.raw.plane_pair),
        FourTwoSingle(R.raw.four_two_single),
        FourTwoPair(R.raw.four_two_pair),
        Bomb(R.raw.bomb),
        Rocket(R.raw.rocket),
        Rank3(R.raw.rank_3),
        Rank4(R.raw.rank_4),
        Rank5(R.raw.rank_5),
        Rank6(R.raw.rank_6),
        Rank7(R.raw.rank_7),
        Rank8(R.raw.rank_8),
        Rank9(R.raw.rank_9),
        Rank10(R.raw.rank_10),
        RankJ(R.raw.rank_j),
        RankQ(R.raw.rank_q),
        RankK(R.raw.rank_k),
        RankA(R.raw.rank_a),
        Rank2(R.raw.rank_2),
        SmallJoker(R.raw.rank_small_joker),
        BigJoker(R.raw.rank_big_joker),
    }
}
