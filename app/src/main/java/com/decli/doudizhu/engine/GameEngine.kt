package com.decli.doudizhu.engine

import com.decli.doudizhu.model.BidAction
import com.decli.doudizhu.model.BidState
import com.decli.doudizhu.model.Card
import com.decli.doudizhu.model.GamePhase
import com.decli.doudizhu.model.GameSession
import com.decli.doudizhu.model.MatchStats
import com.decli.doudizhu.model.PlayedTurn
import com.decli.doudizhu.model.PlayerRole
import com.decli.doudizhu.model.PlayerState
import com.decli.doudizhu.model.Seat
import com.decli.doudizhu.model.Suit
import com.decli.doudizhu.model.player
import com.decli.doudizhu.model.replacePlayer
import com.decli.doudizhu.model.sortCards
import kotlin.random.Random

object GameEngine {
    fun newSession(
        roundIndex: Int = 1,
        stats: MatchStats = MatchStats(),
        randomSeed: Long = System.currentTimeMillis(),
    ): GameSession {
        val random = Random(randomSeed)
        val deck = standardDeck().shuffled(random)
        val startSeat = Seat.entries.random(random)

        return GameSession(
            phase = GamePhase.Bidding,
            players = listOf(
                PlayerState(Seat.West, "西侧电脑", isHuman = false, hand = sortCards(deck.subList(0, 17))),
                PlayerState(Seat.South, "您", isHuman = true, hand = sortCards(deck.subList(17, 34))),
                PlayerState(Seat.East, "东侧电脑", isHuman = false, hand = sortCards(deck.subList(34, 51))),
            ),
            bottomCards = sortCards(deck.subList(51, 54)),
            currentTurn = startSeat,
            bidState = BidState(currentSeat = startSeat),
            roundIndex = roundIndex,
            stats = stats,
            message = "第${roundIndex}局开始，请叫分",
        )
    }

    fun applyBid(session: GameSession, seat: Seat, score: Int): GameSession {
        require(session.phase == GamePhase.Bidding) { "当前不是叫分阶段" }
        require(session.currentTurn == seat) { "还没有轮到该玩家叫分" }
        require(score in 0..3) { "叫分必须在 0 到 3 之间" }

        val bidState = session.bidState ?: error("叫分状态丢失")
        val actions = bidState.actions + BidAction(seat, score)
        val newHighestScore = maxOf(score, bidState.highestScore)
        val newHighestSeat = if (score > bidState.highestScore) seat else bidState.highestSeat
        val shouldFinalize = score == 3 || actions.size == 3

        if (shouldFinalize) {
            val landlordSeat = newHighestSeat ?: return newSession(
                roundIndex = session.roundIndex + 1,
                stats = session.stats,
            ).copy(message = "三家都不叫，自动重新发牌")

            val landlordPlayer = session.player(landlordSeat)
            var updated = session.copy(
                phase = GamePhase.Playing,
                currentTurn = landlordSeat,
                landlordSeat = landlordSeat,
                bidState = BidState(
                    currentSeat = landlordSeat,
                    highestScore = newHighestScore,
                    highestSeat = landlordSeat,
                    actions = actions,
                ),
                calledScore = newHighestScore,
                message = "${seatLabel(landlordSeat)}叫到${newHighestScore}分，成为地主",
            )

            updated.players.forEach { player ->
                updated = updated.replacePlayer(
                    player.copy(
                        role = if (player.seat == landlordSeat) PlayerRole.Landlord else PlayerRole.Farmer,
                        hand = if (player.seat == landlordSeat) sortCards(landlordPlayer.hand + session.bottomCards) else player.hand,
                    ),
                )
            }
            return updated
        }

        val nextSeat = seat.next()
        return session.copy(
            currentTurn = nextSeat,
            bidState = BidState(
                currentSeat = nextSeat,
                highestScore = newHighestScore,
                highestSeat = newHighestSeat,
                actions = actions,
            ),
            message = "${seatLabel(seat)}${if (score == 0) "不叫" else "叫${score}分"}",
        )
    }

    fun playCards(session: GameSession, seat: Seat, cards: List<Card>): GameSession {
        require(session.phase == GamePhase.Playing) { "当前不是出牌阶段" }
        require(session.currentTurn == seat) { "还没有轮到该玩家出牌" }
        require(cards.isNotEmpty()) { "请选择要出的牌" }

        val currentPlayer = session.player(seat)
        require(cards.all { selected -> currentPlayer.hand.any { it.id == selected.id } }) { "所选牌不在当前手牌中" }

        val combo = RuleEngine.identifyCombo(cards) ?: error("不符合斗地主牌型")
        val target = session.lastPlayedTurn?.takeIf { it.seat != seat }?.let { RuleEngine.identifyCombo(it.cards) }
        if (target != null && !RuleEngine.canBeat(combo, target)) error("必须压过上一手")

        val remainingHand = currentPlayer.hand.filterNot { handCard -> cards.any { it.id == handCard.id } }
        var updated = session.replacePlayer(currentPlayer.copy(hand = sortCards(remainingHand)))
        updated = updated.copy(
            currentTurn = seat.next(),
            lastPlayedTurn = PlayedTurn(seat, sortCards(cards), CardFormatter.comboLabel(cards)),
            passesInCycle = 0,
            multiplier = if (combo.type == ComboType.Bomb || combo.type == ComboType.Rocket) session.multiplier * 2 else session.multiplier,
            message = "${seatLabel(seat)}出牌：${CardFormatter.comboLabel(cards)}",
        )

        if (remainingHand.isEmpty()) {
            val humanWon = seat == Seat.South
            val landlordWon = seat == updated.landlordSeat
            return updated.copy(
                phase = GamePhase.Finished,
                winner = seat,
                stats = updated.stats.copy(
                    playedRounds = updated.stats.playedRounds + 1,
                    humanWins = updated.stats.humanWins + if (humanWon) 1 else 0,
                    landlordWins = updated.stats.landlordWins + if (landlordWon) 1 else 0,
                ),
                message = "${seatLabel(seat)}赢下本局",
            )
        }
        return updated
    }

    fun pass(session: GameSession, seat: Seat): GameSession {
        require(session.phase == GamePhase.Playing) { "当前不是出牌阶段" }
        require(session.currentTurn == seat) { "还没有轮到该玩家" }
        val lastTurn = session.lastPlayedTurn ?: error("当前轮次不能不出")
        require(lastTurn.seat != seat) { "起牌玩家不能不出" }

        val newPasses = session.passesInCycle + 1
        return if (newPasses >= 2) {
            session.copy(
                currentTurn = lastTurn.seat,
                lastPlayedTurn = null,
                passesInCycle = 0,
                message = "两家不出，${seatLabel(lastTurn.seat)}重新起牌",
            )
        } else {
            session.copy(
                currentTurn = seat.next(),
                passesInCycle = newPasses,
                message = "${seatLabel(seat)}选择不出",
            )
        }
    }

    fun seatLabel(seat: Seat): String = when (seat) {
        Seat.West -> "西侧电脑"
        Seat.South -> "您"
        Seat.East -> "东侧电脑"
    }

    private fun standardDeck(): List<Card> {
        val suits = listOf(Suit.Spade, Suit.Heart, Suit.Club, Suit.Diamond)
        val cards = mutableListOf<Card>()
        var index = 0
        for (rank in 3..15) {
            suits.forEach { suit ->
                cards += Card(index++, rank, suit)
            }
        }
        cards += Card(index++, 16, Suit.Joker)
        cards += Card(index, 17, Suit.Joker)
        return cards
    }
}
