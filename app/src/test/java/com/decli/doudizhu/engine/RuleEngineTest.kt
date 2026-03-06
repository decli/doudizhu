package com.decli.doudizhu.engine

import com.decli.doudizhu.model.Card
import com.decli.doudizhu.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    @Test
    fun identifiesRocket() {
        val cards = listOf(
            Card(1, 16, Suit.Joker),
            Card(2, 17, Suit.Joker),
        )

        val combo = RuleEngine.identifyCombo(cards)

        assertEquals(ComboType.Rocket, combo?.type)
    }

    @Test
    fun identifiesPlaneWithSingles() {
        val cards = listOf(
            Card(1, 3, Suit.Spade),
            Card(2, 3, Suit.Heart),
            Card(3, 3, Suit.Club),
            Card(4, 4, Suit.Spade),
            Card(5, 4, Suit.Heart),
            Card(6, 4, Suit.Club),
            Card(7, 7, Suit.Spade),
            Card(8, 8, Suit.Spade),
        )

        val combo = RuleEngine.identifyCombo(cards)

        assertEquals(ComboType.PlaneSingle, combo?.type)
        assertEquals(2, combo?.chainLength)
    }

    @Test
    fun bombsBeatAnyNonBomb() {
        val bomb = RuleEngine.identifyCombo(
            listOf(
                Card(1, 9, Suit.Spade),
                Card(2, 9, Suit.Heart),
                Card(3, 9, Suit.Club),
                Card(4, 9, Suit.Diamond),
            ),
        )!!
        val straight = RuleEngine.identifyCombo(
            listOf(
                Card(5, 3, Suit.Spade),
                Card(6, 4, Suit.Heart),
                Card(7, 5, Suit.Club),
                Card(8, 6, Suit.Diamond),
                Card(9, 7, Suit.Spade),
            ),
        )!!

        assertTrue(RuleEngine.canBeat(bomb, straight))
    }
}

