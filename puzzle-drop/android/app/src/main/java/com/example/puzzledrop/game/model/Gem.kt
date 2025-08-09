package com.example.puzzledrop.game.model

import androidx.compose.ui.graphics.Color
import com.example.puzzledrop.ui.theme.*

enum class GemType(val color: Color) {
    Red(GemRed),
    Orange(GemOrange),
    Yellow(GemYellow),
    Green(GemGreen),
    Blue(GemBlue),
    Purple(GemPurple);

    companion object {
        val entriesList: List<GemType> = entries
    }
}

data class Position(val row: Int, val col: Int) {
    fun isAdjacentTo(other: Position): Boolean {
        val dr = kotlin.math.abs(row - other.row)
        val dc = kotlin.math.abs(col - other.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }
}

/**
 * Each Gem has a stable [id] used as a Compose key for smooth animations.
 */
data class Gem(
    val id: Long,
    val type: GemType,
    val row: Int,
    val col: Int,
    val removing: Boolean = false
)