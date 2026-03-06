package com.decli.doudizhu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.decli.doudizhu.audio.RobotAnnouncer
import com.decli.doudizhu.data.GameRepository
import com.decli.doudizhu.engine.AiEngine
import com.decli.doudizhu.engine.GameEngine
import com.decli.doudizhu.model.GamePhase
import com.decli.doudizhu.model.GameSession
import com.decli.doudizhu.model.Seat
import com.decli.doudizhu.model.player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val session: GameSession? = null,
    val selectedCardIds: Set<Int> = emptySet(),
    val restoredFromSave: Boolean = false,
    val highlightedCardIds: Set<Int> = emptySet(),
)

class GameViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val announcer = RobotAnnouncer(application)
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            val restored = repository.loadSession()
            val session = restored ?: GameEngine.newSession()
            _uiState.value = GameUiState(session = session, restoredFromSave = restored != null)
            if (restored == null) repository.saveSession(session)
            scheduleAiTurn()
        }
    }

    fun newGame() {
        val current = _uiState.value.session ?: return
        commit(
            GameEngine.newSession(
                roundIndex = current.roundIndex + 1,
                stats = current.stats,
            ),
        )
    }

    fun toggleCardSelection(cardId: Int) {
        val session = _uiState.value.session ?: return
        if (session.phase != GamePhase.Playing || session.currentTurn != Seat.South) return
        val selected = _uiState.value.selectedCardIds.toMutableSet()
        if (!selected.add(cardId)) selected.remove(cardId)
        _uiState.value = _uiState.value.copy(selectedCardIds = selected, highlightedCardIds = emptySet())
    }

    fun hint() {
        val session = _uiState.value.session ?: return
        val suggestion = AiEngine.suggestPlay(session, Seat.South) ?: return
        val ids = suggestion.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedCardIds = ids, highlightedCardIds = ids)
    }

    fun playSelected() {
        val session = _uiState.value.session ?: return
        val cards = session.player(Seat.South).hand.filter { it.id in _uiState.value.selectedCardIds }
        runCatching { GameEngine.playCards(session, Seat.South, cards) }
            .onSuccess(::commit)
            .onFailure { setMessage(it.message ?: "出牌失败") }
    }

    fun pass() {
        val session = _uiState.value.session ?: return
        runCatching { GameEngine.pass(session, Seat.South) }
            .onSuccess(::commit)
            .onFailure { setMessage(it.message ?: "当前不能不出") }
    }

    fun bid(score: Int) {
        val session = _uiState.value.session ?: return
        runCatching { GameEngine.applyBid(session, Seat.South, score) }
            .onSuccess(::commit)
            .onFailure { setMessage(it.message ?: "叫分失败") }
    }

    private fun commit(session: GameSession) {
        _uiState.value = _uiState.value.copy(
            session = session,
            selectedCardIds = emptySet(),
            highlightedCardIds = emptySet(),
            restoredFromSave = false,
        )
        viewModelScope.launch {
            repository.saveSession(session)
        }
        scheduleAiTurn()
    }

    private fun setMessage(message: String) {
        val session = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(session = session.copy(message = message))
    }

    private fun scheduleAiTurn() {
        aiJob?.cancel()
        val session = _uiState.value.session ?: return
        if (session.phase == GamePhase.Finished) return
        val currentPlayer = session.player(session.currentTurn)
        if (currentPlayer.isHuman) return

        aiJob = viewModelScope.launch {
            delay(900)
            val latest = _uiState.value.session ?: return@launch
            if (latest.currentTurn != currentPlayer.seat) return@launch
            when (latest.phase) {
                GamePhase.Bidding -> commit(GameEngine.applyBid(latest, currentPlayer.seat, AiEngine.chooseBid(latest, currentPlayer.seat)))
                GamePhase.Playing -> {
                    val action = AiEngine.choosePlay(latest, currentPlayer.seat)
                    val updated = if (action == null) {
                        GameEngine.pass(latest, currentPlayer.seat)
                    } else {
                        announcer.announce(action)
                        GameEngine.playCards(latest, currentPlayer.seat, action)
                    }
                    commit(updated)
                }

                GamePhase.Finished -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        announcer.release()
    }
}
