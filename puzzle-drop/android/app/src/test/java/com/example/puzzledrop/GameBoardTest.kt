package com.example.puzzledrop

import com.example.puzzledrop.game.model.GameBoard
import com.example.puzzledrop.game.model.Gem
import com.example.puzzledrop.game.model.GemType
import com.example.puzzledrop.game.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameBoardTest {
    @Test
    fun initialize_noImmediateMatches() {
        val board = GameBoard(8, 8, Random(123))
        board.initializeWithoutMatches()
        assertTrue(board.findMatches().isEmpty())
    }

    @Test
    fun swap_and_match_produces_match() {
        val board = GameBoard(3, 3, Random(1))
        // Force a near-match layout
        // R R G
        // B G G
        // B Y Y
        fun g(id: Long, t: GemType, r: Int, c: Int) = Gem(id, t, r, c)
        board.grid = arrayOf(
            arrayOf(g(1, GemType.Red, 0, 0), g(2, GemType.Red, 0, 1), g(3, GemType.Green, 0, 2)),
            arrayOf(g(4, GemType.Blue, 1, 0), g(5, GemType.Green, 1, 1), g(6, GemType.Green, 1, 2)),
            arrayOf(g(7, GemType.Blue, 2, 0), g(8, GemType.Yellow, 2, 1), g(9, GemType.Yellow, 2, 2)),
        )

        // Swap (0,2) with (1,2) to form vertical greens
        board.swap(Position(0,2), Position(1,2))
        val matches = board.findMatches()
        assertTrue(matches.isNotEmpty())
    }
}