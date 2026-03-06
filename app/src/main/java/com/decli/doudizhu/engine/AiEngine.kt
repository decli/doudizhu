package com.decli.doudizhu.engine

import com.decli.doudizhu.model.Card
import com.decli.doudizhu.model.GameSession
import com.decli.doudizhu.model.Seat
import com.decli.doudizhu.model.player

object AiEngine {
    private val memo = mutableMapOf<String, Int>()

    fun chooseBid(session: GameSession, seat: Seat): Int {
        val hand = session.player(seat).hand
        val openingCombos = RuleEngine.generateOpeningCombos(hand)
        val bombCount = openingCombos.count { it.type == ComboType.Bomb } + openingCombos.count { it.type == ComboType.Rocket }
        val control = hand.fold(0) { acc, card ->
            acc + when {
                card.rank >= 16 -> 6
                card.rank == 15 -> 5
                card.rank == 14 -> 3
                card.rank == 13 -> 2
                else -> 0
            }
        }
        val strength = bombCount * 12 + control - evaluateHand(hand)
        return when {
            strength >= 18 -> 3
            strength >= 8 -> 2
            strength >= 1 -> 1
            else -> 0
        }
    }

    fun choosePlay(session: GameSession, seat: Seat): List<Card>? {
        val hand = session.player(seat).hand
        val lastTurn = session.lastPlayedTurn
        val target = lastTurn?.takeIf { it.seat != seat }?.let { RuleEngine.identifyCombo(it.cards) }

        if (target == null) return chooseLeading(hand)

        val responses = RuleEngine.generateResponses(hand, target)
        if (responses.isEmpty()) return null

        val landlordSeat = session.landlordSeat
        val teammateWinning = landlordSeat != null &&
            landlordSeat != seat &&
            lastTurn.seat != landlordSeat &&
            lastTurn.seat != seat

        if (teammateWinning && responses.none { it.cards.size == hand.size }) {
            return null
        }

        return responses.minByOrNull { responseCost(hand, it.cards) }?.cards
    }

    fun suggestPlay(session: GameSession, seat: Seat): List<Card>? = choosePlay(session, seat)

    private fun chooseLeading(hand: List<Card>): List<Card> {
        val candidates = RuleEngine.generateOpeningCombos(hand)
        candidates.firstOrNull { it.cards.size == hand.size }?.let { return it.cards }
        return candidates.minByOrNull { responseCost(hand, it.cards) }?.cards ?: listOf(hand.minBy { it.rank })
    }

    private fun responseCost(hand: List<Card>, cardsToPlay: List<Card>): Int {
        val remaining = hand.filterNot { owned -> cardsToPlay.any { it.id == owned.id } }
        val combo = RuleEngine.identifyCombo(cardsToPlay)
        val comboBias = when (combo?.type) {
            ComboType.Bomb -> 24
            ComboType.Rocket -> 30
            ComboType.Straight, ComboType.PairStraight, ComboType.Plane, ComboType.PlaneSingle, ComboType.PlanePair -> -4
            else -> 0
        }
        val rankBias = cardsToPlay.fold(0) { acc, card -> acc + (20 - card.rank) }
        return evaluateHand(remaining) + comboBias + rankBias
    }

    private fun evaluateHand(hand: List<Card>): Int {
        if (hand.isEmpty()) return 0
        val signature = hand.groupingBy { it.rank }.eachCount().entries.joinToString("|") { "${it.key}:${it.value}" }
        memo[signature]?.let { return it }

        var best = roughPenalty(hand)
        val candidates = RuleEngine.generateOpeningCombos(hand)
            .sortedWith(compareByDescending<CardCombo> { it.cards.size }.thenBy { it.primaryRank })
            .take(24)

        for (combo in candidates) {
            val remaining = hand.filterNot { owned -> combo.cards.any { it.id == owned.id } }
            best = minOf(best, 12 + evaluateHand(remaining))
        }

        memo[signature] = best
        return best
    }

    private fun roughPenalty(hand: List<Card>): Int {
        val byRank = hand.groupingBy { it.rank }.eachCount()
        val singles = byRank.count { it.value == 1 }
        val highs = hand.fold(0) { acc, card ->
            acc + when {
                card.rank >= 16 -> 8
                card.rank == 15 -> 5
                card.rank == 14 -> 3
                else -> 0
            }
        }
        return singles * 14 + highs + hand.size
    }
}
