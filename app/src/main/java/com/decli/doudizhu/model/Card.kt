package com.decli.doudizhu.model

import kotlinx.serialization.Serializable

@Serializable
enum class Suit {
    Spade,
    Heart,
    Club,
    Diamond,
    Joker,
}

@Serializable
enum class Seat {
    West,
    South,
    East,
    ;

    fun next(): Seat = when (this) {
        West -> South
        South -> East
        East -> West
    }
}

@Serializable
enum class PlayerRole {
    Landlord,
    Farmer,
}

@Serializable
enum class GamePhase {
    Bidding,
    Playing,
    Finished,
}

@Serializable
data class Card(
    val id: Int,
    val rank: Int,
    val suit: Suit,
)

@Serializable
data class PlayerState(
    val seat: Seat,
    val name: String,
    val isHuman: Boolean,
    val hand: List<Card>,
    val role: PlayerRole? = null,
)

@Serializable
data class BidAction(
    val seat: Seat,
    val score: Int,
)

@Serializable
data class BidState(
    val currentSeat: Seat,
    val highestScore: Int = 0,
    val highestSeat: Seat? = null,
    val actions: List<BidAction> = emptyList(),
)

@Serializable
data class PlayedTurn(
    val seat: Seat,
    val cards: List<Card>,
    val description: String,
)

@Serializable
data class MatchStats(
    val playedRounds: Int = 0,
    val humanWins: Int = 0,
    val landlordWins: Int = 0,
)

@Serializable
data class GameSession(
    val phase: GamePhase,
    val players: List<PlayerState>,
    val bottomCards: List<Card>,
    val currentTurn: Seat,
    val landlordSeat: Seat? = null,
    val bidState: BidState? = null,
    val lastPlayedTurn: PlayedTurn? = null,
    val passesInCycle: Int = 0,
    val multiplier: Int = 1,
    val calledScore: Int = 0,
    val roundIndex: Int = 1,
    val winner: Seat? = null,
    val message: String = "",
    val stats: MatchStats = MatchStats(),
)

fun GameSession.player(seat: Seat): PlayerState = players.first { it.seat == seat }

fun GameSession.replacePlayer(updated: PlayerState): GameSession =
    copy(players = players.map { if (it.seat == updated.seat) updated else it })

fun rankOrder(rank: Int): Int = when (rank) {
    16 -> 16
    17 -> 17
    else -> rank
}

fun sortCards(cards: List<Card>): List<Card> =
    cards.sortedWith(
        compareByDescending<Card> { rankOrder(it.rank) }
            .thenByDescending { suitWeight(it.suit) },
    )

private fun suitWeight(suit: Suit): Int = when (suit) {
    Suit.Spade -> 4
    Suit.Heart -> 3
    Suit.Club -> 2
    Suit.Diamond -> 1
    Suit.Joker -> 5
}
