package com.decli.doudizhu.engine

import com.decli.doudizhu.model.Card
import com.decli.doudizhu.model.sortCards

enum class ComboType {
    Single,
    Pair,
    Triple,
    TripleSingle,
    TriplePair,
    Straight,
    PairStraight,
    Plane,
    PlaneSingle,
    PlanePair,
    FourTwoSingle,
    FourTwoPair,
    Bomb,
    Rocket,
}

data class CardCombo(
    val type: ComboType,
    val cards: List<Card>,
    val primaryRank: Int,
    val chainLength: Int = 1,
)

object RuleEngine {
    fun identifyCombo(cards: List<Card>): CardCombo? {
        if (cards.isEmpty()) return null
        val sorted = sortCards(cards)
        val countMap = sorted.groupingBy { it.rank }.eachCount().toSortedMap()
        val ranks = countMap.keys.toList()
        val counts = countMap.values.sortedDescending()
        val size = sorted.size

        if (size == 1) return CardCombo(ComboType.Single, sorted, ranks.first())
        if (size == 2) {
            if (ranks.contains(16) && ranks.contains(17)) return CardCombo(ComboType.Rocket, sorted, 17)
            if (counts == listOf(2)) return CardCombo(ComboType.Pair, sorted, ranks.first())
        }
        if (size == 3 && counts == listOf(3)) return CardCombo(ComboType.Triple, sorted, rankOfCount(countMap, 3))
        if (size == 4) {
            if (counts == listOf(4)) return CardCombo(ComboType.Bomb, sorted, rankOfCount(countMap, 4))
            if (counts == listOf(3, 1)) return CardCombo(ComboType.TripleSingle, sorted, rankOfCount(countMap, 3))
        }
        if (size == 5 && counts == listOf(3, 2)) return CardCombo(ComboType.TriplePair, sorted, rankOfCount(countMap, 3))
        if (isStraight(countMap, 1, 5)) return CardCombo(ComboType.Straight, sorted, ranks.max(), size)
        if (size % 2 == 0 && isStraight(countMap, 2, 3)) return CardCombo(ComboType.PairStraight, sorted, ranks.max(), size / 2)

        findPlaneCombo(countMap, sorted)?.let { return it }
        findFourTwoCombo(countMap, sorted)?.let { return it }
        return null
    }

    fun canBeat(candidate: CardCombo, target: CardCombo): Boolean {
        if (candidate.type == ComboType.Rocket) return true
        if (target.type == ComboType.Rocket) return false
        if (candidate.type == ComboType.Bomb && target.type != ComboType.Bomb) return true
        if (candidate.type != target.type) return false
        if (candidate.type == ComboType.Bomb) return candidate.primaryRank > target.primaryRank
        if (candidate.cards.size != target.cards.size) return false
        if (candidate.chainLength != target.chainLength) return false
        return candidate.primaryRank > target.primaryRank
    }

    fun generateOpeningCombos(hand: List<Card>): List<CardCombo> {
        val byRank = grouped(hand)
        val combos = linkedMapOf<String, CardCombo>()

        fun add(combo: CardCombo?) {
            if (combo == null) return
            combos.putIfAbsent(signature(combo.cards), combo)
        }

        byRank.values.forEach { cards ->
            add(CardCombo(ComboType.Single, listOf(cards.first()), cards.first().rank))
            if (cards.size >= 2) add(CardCombo(ComboType.Pair, cards.take(2), cards.first().rank))
            if (cards.size >= 3) add(CardCombo(ComboType.Triple, cards.take(3), cards.first().rank))
            if (cards.size == 4) add(CardCombo(ComboType.Bomb, cards, cards.first().rank))
        }

        val rocketCards = listOfNotNull(byRank[16]?.firstOrNull(), byRank[17]?.firstOrNull())
        if (rocketCards.size == 2) add(CardCombo(ComboType.Rocket, sortCards(rocketCards), 17))

        generateStraights(byRank, singleMode = true).forEach(::add)
        generateStraights(byRank, singleMode = false).forEach(::add)
        generateTriplesWithAttachments(byRank).forEach(::add)
        generatePlanes(byRank).forEach(::add)
        generateFourWithAttachments(byRank).forEach(::add)

        return combos.values.toList()
    }

    fun generateResponses(hand: List<Card>, target: CardCombo): List<CardCombo> {
        val byRank = grouped(hand)
        val combos = mutableListOf<CardCombo>()

        when (target.type) {
            ComboType.Single -> byRank.filterKeys { it > target.primaryRank }.values.forEach { cards ->
                combos += CardCombo(ComboType.Single, listOf(cards.first()), cards.first().rank)
            }

            ComboType.Pair -> byRank.filter { it.key > target.primaryRank && it.value.size >= 2 }.values.forEach { cards ->
                combos += CardCombo(ComboType.Pair, cards.take(2), cards.first().rank)
            }

            ComboType.Triple -> byRank.filter { it.key > target.primaryRank && it.value.size >= 3 }.values.forEach { cards ->
                combos += CardCombo(ComboType.Triple, cards.take(3), cards.first().rank)
            }

            ComboType.TripleSingle -> {
                triplesAbove(byRank, target.primaryRank).forEach { triple ->
                    removeCards(hand, triple).forEach { attachment ->
                        combos += CardCombo(ComboType.TripleSingle, sortCards(triple + attachment), triple.first().rank)
                    }
                }
            }

            ComboType.TriplePair -> {
                triplesAbove(byRank, target.primaryRank).forEach { triple ->
                    grouped(removeCards(hand, triple)).values.filter { it.size >= 2 }.forEach { pair ->
                        combos += CardCombo(ComboType.TriplePair, sortCards(triple + pair.take(2)), triple.first().rank)
                    }
                }
            }

            ComboType.Straight -> generateStraights(byRank, true, target.chainLength).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.PairStraight -> generateStraights(byRank, false, target.chainLength).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.Plane -> generatePlanes(byRank, target.chainLength, ComboType.Plane).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.PlaneSingle -> generatePlanes(byRank, target.chainLength, ComboType.PlaneSingle).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.PlanePair -> generatePlanes(byRank, target.chainLength, ComboType.PlanePair).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.FourTwoSingle -> generateFourWithAttachments(byRank, ComboType.FourTwoSingle).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.FourTwoPair -> generateFourWithAttachments(byRank, ComboType.FourTwoPair).filter { it.primaryRank > target.primaryRank }.forEach { combos += it }
            ComboType.Bomb -> byRank.filter { it.key > target.primaryRank && it.value.size == 4 }.values.forEach { cards ->
                combos += CardCombo(ComboType.Bomb, cards, cards.first().rank)
            }

            ComboType.Rocket -> Unit
        }

        if (target.type != ComboType.Rocket) {
            byRank.values.filter { it.size == 4 }.forEach { cards ->
                val bomb = CardCombo(ComboType.Bomb, cards, cards.first().rank)
                if (target.type != ComboType.Bomb || bomb.primaryRank > target.primaryRank) combos += bomb
            }
            val rocketCards = listOfNotNull(byRank[16]?.firstOrNull(), byRank[17]?.firstOrNull())
            if (rocketCards.size == 2) combos += CardCombo(ComboType.Rocket, sortCards(rocketCards), 17)
        }

        return combos
            .distinctBy { signature(it.cards) }
            .filter { canBeat(it, target) }
            .sortedWith(compareBy<CardCombo> { comboWeight(it.type) }.thenBy { it.primaryRank }.thenBy { it.cards.size })
    }

    private fun generateStraights(
        byRank: Map<Int, List<Card>>,
        singleMode: Boolean,
        exactLength: Int? = null,
    ): List<CardCombo> {
        val copies = if (singleMode) 1 else 2
        val minLength = if (singleMode) 5 else 3
        val validRanks = (3..14).filter { (byRank[it]?.size ?: 0) >= copies }
        val sequences = consecutiveSequences(validRanks)
        val combos = mutableListOf<CardCombo>()
        val type = if (singleMode) ComboType.Straight else ComboType.PairStraight

        for (sequence in sequences) {
            val lengths = exactLength?.let { listOf(it) } ?: (minLength..sequence.size)
            for (length in lengths) {
                if (length > sequence.size) continue
                for (start in 0..sequence.size - length) {
                    val slice = sequence.subList(start, start + length)
                    val cards = slice.flatMap { rank ->
                        if (singleMode) listOf(byRank.getValue(rank).first()) else byRank.getValue(rank).take(2)
                    }
                    combos += CardCombo(type, sortCards(cards), slice.last(), length)
                }
            }
        }
        return combos
    }

    private fun generateTriplesWithAttachments(byRank: Map<Int, List<Card>>): List<CardCombo> {
        val triples = byRank.values.filter { it.size >= 3 }.map { it.take(3) }
        val combos = mutableListOf<CardCombo>()
        triples.forEach { triple ->
            val remaining = removeCards(byRank.values.flatten(), triple)
            remaining.forEach { single ->
                combos += CardCombo(ComboType.TripleSingle, sortCards(triple + single), triple.first().rank)
            }
            grouped(remaining).values.filter { it.size >= 2 }.forEach { pair ->
                combos += CardCombo(ComboType.TriplePair, sortCards(triple + pair.take(2)), triple.first().rank)
            }
        }
        return combos
    }

    private fun generatePlanes(
        byRank: Map<Int, List<Card>>,
        exactLength: Int? = null,
        mode: ComboType? = null,
    ): List<CardCombo> {
        val tripleRanks = (3..14).filter { (byRank[it]?.size ?: 0) >= 3 }
        val sequences = consecutiveSequences(tripleRanks).filter { it.size >= 2 }
        val combos = mutableListOf<CardCombo>()

        for (sequence in sequences) {
            val lengths = exactLength?.let { listOf(it) } ?: (2..sequence.size)
            for (length in lengths) {
                if (length > sequence.size) continue
                for (start in 0..sequence.size - length) {
                    val slice = sequence.subList(start, start + length)
                    val body = slice.flatMap { byRank.getValue(it).take(3) }
                    val bodyIds = body.map(Card::id).toSet()
                    val remaining = byRank.values.flatten().filterNot { it.id in bodyIds }

                    if (mode == null || mode == ComboType.Plane) {
                        combos += CardCombo(ComboType.Plane, sortCards(body), slice.last(), length)
                    }
                    if (mode == null || mode == ComboType.PlaneSingle) {
                        remaining.combinations(length).forEach { singles ->
                            combos += CardCombo(ComboType.PlaneSingle, sortCards(body + singles), slice.last(), length)
                        }
                    }
                    if (mode == null || mode == ComboType.PlanePair) {
                        grouped(remaining).values.filter { it.size >= 2 }.map { it.take(2) }.combinations(length).forEach { pairs ->
                            combos += CardCombo(ComboType.PlanePair, sortCards(body + pairs.flatten()), slice.last(), length)
                        }
                    }
                }
            }
        }
        return combos.filter { identifyCombo(it.cards)?.type == it.type }.distinctBy { signature(it.cards) }
    }

    private fun generateFourWithAttachments(
        byRank: Map<Int, List<Card>>,
        mode: ComboType? = null,
    ): List<CardCombo> {
        val combos = mutableListOf<CardCombo>()
        byRank.values.filter { it.size == 4 }.forEach { bomb ->
            val remaining = removeCards(byRank.values.flatten(), bomb)
            if (mode == null || mode == ComboType.FourTwoSingle) {
                remaining.combinations(2).forEach { singles ->
                    combos += CardCombo(ComboType.FourTwoSingle, sortCards(bomb + singles), bomb.first().rank)
                }
            }
            if (mode == null || mode == ComboType.FourTwoPair) {
                grouped(remaining).values.filter { it.size >= 2 }.map { it.take(2) }.combinations(2).forEach { pairs ->
                    combos += CardCombo(ComboType.FourTwoPair, sortCards(bomb + pairs.flatten()), bomb.first().rank)
                }
            }
        }
        return combos.filter { identifyCombo(it.cards)?.type == it.type }.distinctBy { signature(it.cards) }
    }

    private fun findPlaneCombo(countMap: Map<Int, Int>, cards: List<Card>): CardCombo? {
        val tripleRanks = countMap.filter { it.key < 15 && it.value >= 3 }.keys.sorted()
        val sequences = consecutiveSequences(tripleRanks).sortedByDescending { it.size }
        for (sequence in sequences) {
            for (length in sequence.size downTo 2) {
                for (start in 0..sequence.size - length) {
                    val slice = sequence.subList(start, start + length)
                    val mutable = countMap.toMutableMap()
                    slice.forEach { mutable[it] = mutable.getValue(it) - 3 }
                    val remaining = mutable.filterValues { it > 0 }
                    val total = remaining.values.sum()
                    if (total == 0) return CardCombo(ComboType.Plane, cards, slice.last(), length)
                    if (total == length) return CardCombo(ComboType.PlaneSingle, cards, slice.last(), length)
                    if (total == length * 2 && remaining.values.all { it % 2 == 0 }) return CardCombo(ComboType.PlanePair, cards, slice.last(), length)
                }
            }
        }
        return null
    }

    private fun findFourTwoCombo(countMap: Map<Int, Int>, cards: List<Card>): CardCombo? {
        val fourRank = countMap.entries.firstOrNull { it.value == 4 }?.key ?: return null
        val remaining = countMap.filterKeys { it != fourRank }
        return when {
            cards.size == 6 && remaining.values.sum() == 2 -> CardCombo(ComboType.FourTwoSingle, cards, fourRank)
            cards.size == 8 && remaining.values.sum() == 4 && remaining.values.all { it == 2 || it == 4 } -> CardCombo(ComboType.FourTwoPair, cards, fourRank)
            else -> null
        }
    }

    private fun isStraight(countMap: Map<Int, Int>, copies: Int, minLength: Int): Boolean {
        val ranks = countMap.keys.sorted()
        if (ranks.size < minLength || ranks.any { it >= 15 }) return false
        if (countMap.values.any { it != copies }) return false
        return ranks.zipWithNext().all { (a, b) -> b == a + 1 }
    }

    private fun rankOfCount(countMap: Map<Int, Int>, count: Int): Int =
        countMap.entries.first { it.value == count }.key

    private fun grouped(cards: List<Card>): Map<Int, List<Card>> =
        cards.groupBy { it.rank }.toSortedMap().mapValues { sortCards(it.value) }

    private fun removeCards(source: List<Card>, used: List<Card>): List<Card> {
        val ids = used.map(Card::id).toSet()
        return source.filterNot { it.id in ids }
    }

    private fun triplesAbove(byRank: Map<Int, List<Card>>, primaryRank: Int): List<List<Card>> =
        byRank.filter { it.key > primaryRank && it.value.size >= 3 }.values.map { it.take(3) }

    private fun consecutiveSequences(ranks: List<Int>): List<List<Int>> {
        if (ranks.isEmpty()) return emptyList()
        val result = mutableListOf<List<Int>>()
        var start = 0
        for (index in 1..ranks.size) {
            val breakHere = index == ranks.size || ranks[index] != ranks[index - 1] + 1
            if (breakHere) {
                result += ranks.subList(start, index)
                start = index
            }
        }
        return result
    }

    private fun signature(cards: List<Card>): String = cards.map { it.id }.sorted().joinToString("-")

    private fun comboWeight(type: ComboType): Int = when (type) {
        ComboType.Single -> 1
        ComboType.Pair -> 2
        ComboType.Triple -> 3
        ComboType.TripleSingle -> 4
        ComboType.TriplePair -> 5
        ComboType.Straight -> 6
        ComboType.PairStraight -> 7
        ComboType.Plane -> 8
        ComboType.PlaneSingle -> 9
        ComboType.PlanePair -> 10
        ComboType.FourTwoSingle -> 11
        ComboType.FourTwoPair -> 12
        ComboType.Bomb -> 13
        ComboType.Rocket -> 14
    }
}

private fun <T> List<T>.combinations(size: Int): List<List<T>> {
    if (size == 0) return listOf(emptyList())
    if (size > this.size) return emptyList()
    val result = mutableListOf<List<T>>()

    fun combine(start: Int, current: MutableList<T>) {
        if (current.size == size) {
            result += current.toList()
            return
        }
        for (index in start..this.size - (size - current.size)) {
            current += this[index]
            combine(index + 1, current)
            current.removeAt(current.lastIndex)
        }
    }

    combine(0, mutableListOf())
    return result
}
