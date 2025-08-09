package com.example.puzzledrop.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puzzledrop.game.model.GameBoard
import com.example.puzzledrop.game.model.Gem
import com.example.puzzledrop.game.model.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_ROWS = 8
private const val DEFAULT_COLS = 8

data class GameUiState(
    val gems: List<Gem> = emptyList(),
    val score: Int = 0,
    val selected: Position? = null,
    val isAnimating: Boolean = false,
    val movesLeft: Int = 50,
    val cascade: Int = 0
)

class GameViewModel : ViewModel() {
    private val board = GameBoard(rows = DEFAULT_ROWS, cols = DEFAULT_COLS)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        board.initializeWithoutMatches()
        _uiState.value = _uiState.value.copy(gems = board.asList())
    }

    fun onCellTapped(row: Int, col: Int) {
        val current = _uiState.value
        if (current.isAnimating || current.movesLeft <= 0) return
        val pos = Position(row, col)
        val selected = current.selected
        if (selected == null) {
            _uiState.value = current.copy(selected = pos)
            return
        }
        if (selected == pos) {
            _uiState.value = current.copy(selected = null)
            return
        }
        if (!selected.isAdjacentTo(pos)) {
            _uiState.value = current.copy(selected = pos)
            return
        }
        performSwapAndResolve(selected, pos)
    }

    private fun performSwapAndResolve(a: Position, b: Position) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnimating = true, selected = null)

            // Try swap
            board.swap(a, b)
            emitBoard()

            // Give time for swap animation
            delay(animTime())

            if (!board.hasAnyMatches()) {
                // swap back
                board.swap(a, b)
                emitBoard()
                delay(animTime())
                _uiState.value = _uiState.value.copy(isAnimating = false)
                return@launch
            }

            var cascadeMultiplier = 0
            var gainedScore = 0

            // Resolve cascades
            while (true) {
                cascadeMultiplier++
                val result = board.removeMatchesCollapseAndRefill()
                val removedCount = result.removedPositions.size
                if (removedCount == 0) break

                gainedScore += removedCount * 10 * cascadeMultiplier

                // Mark removal visually by briefly waiting, then animate drop
                emitBoard()
                delay(animTime() / 2)

                // After collapse+refill, emit and wait for descent animations
                emitBoard()
                delay(animTime())

                if (!board.hasAnyMatches()) break
            }

            val newMoves = (_uiState.value.movesLeft - 1).coerceAtLeast(0)
            _uiState.value = _uiState.value.copy(
                isAnimating = false,
                score = _uiState.value.score + gainedScore,
                movesLeft = newMoves,
                cascade = cascadeMultiplier
            )
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnimating = true, selected = null)
            val newBoard = GameBoard(rows = board.rows, cols = board.cols)
            newBoard.initializeWithoutMatches()
            // Not replacing reference to keep things simple; copy over
            board.grid = newBoard.grid
            emitBoard()
            delay(animTime())
            _uiState.value = _uiState.value.copy(score = 0, movesLeft = 50, cascade = 0, isAnimating = false)
        }
    }

    private fun emitBoard() {
        _uiState.value = _uiState.value.copy(gems = board.asList())
    }

    private fun animTime(): Duration = 240.milliseconds
}