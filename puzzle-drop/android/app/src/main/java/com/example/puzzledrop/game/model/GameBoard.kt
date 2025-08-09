package com.example.puzzledrop.game.model

import kotlin.random.Random

class GameBoard(
    val rows: Int,
    val cols: Int,
    private val random: Random = Random(System.currentTimeMillis())
) {
    private var nextId: Long = 1L

    var grid: Array<Array<Gem?>> = Array(rows) { arrayOfNulls<Gem?>(cols) }
        private set

    fun clone(): GameBoard {
        val cloned = GameBoard(rows, cols, random)
        cloned.nextId = nextId
        cloned.grid = Array(rows) { r ->
            Array(cols) { c -> grid[r][c]?.copy() }
        }
        return cloned
    }

    fun initializeWithoutMatches() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var gem: Gem
                var attempts = 0
                do {
                    val type = GemType.entriesList.random(random)
                    gem = Gem(id = nextId++, type = type, row = r, col = c)
                    attempts++
                } while ((createsHorizontalRun(r, c, gem.type) || createsVerticalRun(r, c, gem.type)) && attempts < 20)
                grid[r][c] = gem
            }
        }
    }

    private fun createsHorizontalRun(row: Int, col: Int, type: GemType): Boolean {
        if (col >= 2) {
            val a = grid[row][col - 1]?.type
            val b = grid[row][col - 2]?.type
            return a == type && b == type
        }
        return false
    }

    private fun createsVerticalRun(row: Int, col: Int, type: GemType): Boolean {
        if (row >= 2) {
            val a = grid[row - 1][col]?.type
            val b = grid[row - 2][col]?.type
            return a == type && b == type
        }
        return false
    }

    fun swap(a: Position, b: Position) {
        val gemA = grid[a.row][a.col]
        val gemB = grid[b.row][b.col]
        require(gemA != null && gemB != null) { "Cannot swap empty cells" }
        grid[a.row][a.col] = gemB!!.copy(row = a.row, col = a.col)
        grid[b.row][b.col] = gemA!!.copy(row = b.row, col = b.col)
    }

    fun hasAnyMatches(): Boolean = findMatches().isNotEmpty()

    fun findMatches(): Set<Position> {
        val matched = mutableSetOf<Position>()
        // Rows
        for (r in 0 until rows) {
            var runStart = 0
            while (runStart < cols) {
                val t = grid[r][runStart]?.type
                var c = runStart + 1
                while (c < cols && grid[r][c]?.type == t) c++
                val runLen = c - runStart
                if (t != null && runLen >= 3) {
                    for (x in runStart until c) matched.add(Position(r, x))
                }
                runStart = c
            }
        }
        // Columns
        for (c in 0 until cols) {
            var runStart = 0
            while (runStart < rows) {
                val t = grid[runStart][c]?.type
                var r = runStart + 1
                while (r < rows && grid[r][c]?.type == t) r++
                val runLen = r - runStart
                if (t != null && runLen >= 3) {
                    for (y in runStart until r) matched.add(Position(y, c))
                }
                runStart = r
            }
        }
        return matched
    }

    data class CollapseResult(
        val removedPositions: Set<Position>,
        val dropped: List<Gem>,
        val spawned: List<Gem>
    )

    fun removeMatchesCollapseAndRefill(): CollapseResult {
        val toRemove = findMatches()
        if (toRemove.isEmpty()) return CollapseResult(emptySet(), emptyList(), emptyList())

        // Remove
        for (pos in toRemove) grid[pos.row][pos.col] = null

        val dropped = mutableListOf<Gem>()

        // Collapse each column
        for (c in 0 until cols) {
            var writeRow = rows - 1
            for (r in rows - 1 downTo 0) {
                val gem = grid[r][c]
                if (gem != null) {
                    if (r != writeRow) {
                        val moved = gem.copy(row = writeRow, col = c)
                        grid[writeRow][c] = moved
                        dropped.add(moved)
                        grid[r][c] = null
                    }
                    writeRow--
                }
            }
            // Now fill from top
            for (r in writeRow downTo 0) {
                val newGem = Gem(
                    id = nextId++,
                    type = GemType.entriesList.random(random),
                    row = r,
                    col = c
                )
                grid[r][c] = newGem
            }
        }

        // Determine spawned gems: those whose ids were newly assigned in empty slots
        val spawned = mutableListOf<Gem>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val g = grid[r][c]
                if (g != null) {
                    // Rough heuristic: new gems have recent ids and previously null positions
                    // In UI we will treat any gem that appeared in a previously-empty row as spawned from above
                    // We'll just include any gem whose id is >= (nextId - rows*cols) and has no predecessor in the same position
                    if (g.id >= nextId - (rows * cols)) spawned.add(g)
                }
            }
        }
        return CollapseResult(removedPositions = toRemove, dropped = dropped, spawned = spawned)
    }

    fun asList(): List<Gem> = buildList {
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c]?.let { add(it) }
        }
    }
}