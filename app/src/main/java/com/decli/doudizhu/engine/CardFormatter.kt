package com.decli.doudizhu.engine

import com.decli.doudizhu.model.Card

object CardFormatter {
    fun rankLabel(rank: Int): String = when (rank) {
        in 3..10 -> rank.toString()
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        14 -> "A"
        15 -> "2"
        16 -> "小王"
        17 -> "大王"
        else -> rank.toString()
    }

    fun comboLabel(cards: List<Card>): String {
        val combo = RuleEngine.identifyCombo(cards) ?: return cards.joinToString(" ") { rankLabel(it.rank) }
        return when (combo.type) {
            ComboType.Single -> rankLabel(combo.primaryRank)
            ComboType.Pair -> "对${rankLabel(combo.primaryRank)}"
            ComboType.Triple -> "三张${rankLabel(combo.primaryRank)}"
            ComboType.TripleSingle -> "三带一${rankLabel(combo.primaryRank)}"
            ComboType.TriplePair -> "三带对${rankLabel(combo.primaryRank)}"
            ComboType.Straight -> "顺子${rankLabel(cards.minOf { it.rank })}到${rankLabel(combo.primaryRank)}"
            ComboType.PairStraight -> "连对${rankLabel(cards.minOf { it.rank })}到${rankLabel(combo.primaryRank)}"
            ComboType.Plane -> "飞机"
            ComboType.PlaneSingle -> "飞机带单"
            ComboType.PlanePair -> "飞机带对"
            ComboType.FourTwoSingle -> "四带二"
            ComboType.FourTwoPair -> "四带两对"
            ComboType.Bomb -> "${rankLabel(combo.primaryRank)}炸弹"
            ComboType.Rocket -> "王炸"
        }
    }

    fun aiAnnouncement(cards: List<Card>): String {
        val combo = RuleEngine.identifyCombo(cards) ?: return "出牌"
        return when (combo.type) {
            ComboType.Single -> rankLabel(combo.primaryRank)
            ComboType.Pair -> "对子${rankLabel(combo.primaryRank)}"
            ComboType.Triple -> "三张${rankLabel(combo.primaryRank)}"
            ComboType.TripleSingle -> "三带一"
            ComboType.TriplePair -> "三带一对"
            ComboType.Straight -> "顺子"
            ComboType.PairStraight -> "连对"
            ComboType.Plane -> "飞机"
            ComboType.PlaneSingle -> "飞机带单"
            ComboType.PlanePair -> "飞机带对"
            ComboType.FourTwoSingle -> "四带二"
            ComboType.FourTwoPair -> "四带两对"
            ComboType.Bomb -> "${rankLabel(combo.primaryRank)}炸弹"
            ComboType.Rocket -> "王炸"
        }
    }
}

