package com.example.puzzledrop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.puzzledrop.game.GameUiState
import com.example.puzzledrop.game.GameViewModel
import com.example.puzzledrop.game.model.Gem
import com.example.puzzledrop.game.model.Position
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        ScoreHeader(state = state, onReset = { viewModel.resetGame() })
        Spacer(Modifier.height(12.dp))
        GameBoardView(
            gems = state.gems,
            rows = 8,
            cols = 8,
            cellSize = 44.dp,
            gridPadding = 6.dp,
            disabled = state.isAnimating,
            onCellTap = { r, c -> viewModel.onCellTapped(r, c) }
        )
        Spacer(Modifier.height(12.dp))
        FooterInfo(state)
    }
}

@Composable
private fun ScoreHeader(state: GameUiState, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Score", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${state.score}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "Moves", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${state.movesLeft}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Button(onClick = onReset, enabled = !state.isAnimating) { Text("Reset") }
    }
}

@Composable
private fun FooterInfo(state: GameUiState) {
    val cascadeInfo = if (state.cascade > 1) " x${state.cascade}" else ""
    Text(
        text = "Cascades$cascadeInfo",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun GameBoardView(
    gems: List<Gem>,
    rows: Int,
    cols: Int,
    cellSize: Dp,
    gridPadding: Dp,
    disabled: Boolean,
    onCellTap: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }
    val paddingPx = with(density) { gridPadding.toPx() }

    val width = cols * cellPx + (cols + 1) * paddingPx
    val height = rows * cellPx + (rows + 1) * paddingPx

    // Track per-gem animatable offsets in pixels
    val animOffsets = remember { mutableStateMapOf<Long, Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>>() }

    Box(
        modifier = Modifier
            .size(width = with(density) { width.toDp() }, height = with(density) { height.toDp() })
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(0.dp)
    ) {
        // Draw grid background
        Canvas(Modifier.matchParentSize()) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = paddingPx + c * (cellPx + paddingPx)
                    val y = paddingPx + r * (cellPx + paddingPx)
                    drawRoundRect(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(cellPx, cellPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellPx * 0.2f, cellPx * 0.2f)
                    )
                }
            }
        }

        gems.forEach { gem ->
            val target = Offset(
                x = paddingPx + gem.col * (cellPx + paddingPx),
                y = paddingPx + gem.row * (cellPx + paddingPx)
            )

            val anim = animOffsets.getOrPut(gem.id) {
                // Start new gems slightly above to create descent animation
                Animatable(Offset(target.x, target.y - cellPx * 1.5f))
            }

            LaunchedEffect(target.x, target.y, gem.id) {
                anim.animateTo(
                    target,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            }

            GemTile(
                color = gem.type.color,
                size = cellSize,
                offset = anim.value,
                onTap = { onCellTap(gem.row, gem.col) },
                disabled = disabled,
                onDragEnd = { delta ->
                    // Interpret drag end to nearest neighbor move
                    val dx = delta.x
                    val dy = delta.y
                    val threshold = cellPx * 0.4f
                    val move = when {
                        abs(dx) > abs(dy) && dx > threshold -> Position(gem.row, gem.col + 1)
                        abs(dx) > abs(dy) && dx < -threshold -> Position(gem.row, gem.col - 1)
                        abs(dy) > abs(dx) && dy > threshold -> Position(gem.row + 1, gem.col)
                        abs(dy) > abs(dx) && dy < -threshold -> Position(gem.row - 1, gem.col)
                        else -> null
                    }
                    move?.let { onCellTap(it.row, it.col) }
                }
            )
        }
    }
}

@Composable
private fun Modifier.pointerTap(disabled: Boolean, onTap: () -> Unit): Modifier =
    if (disabled) this else this.pointerInput(Unit) {
        detectTapGestures(onTap = { onTap() })
    }

@Composable
private fun GemTile(
    color: Color,
    size: Dp,
    offset: Offset,
    disabled: Boolean,
    onTap: () -> Unit,
    onDragEnd: (delta: Offset) -> Unit
) {
    val density = LocalDensity.current
    val pxSize = with(density) { size.toPx() }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(offset.x.toInt(), offset.y.toInt()) }
            .size(size)
            .clip(CircleShape)
            .background(color)
            .pointerInput(disabled) {
                if (!disabled) {
                    detectDragGestures(
                        onDragStart = { dragDelta = Offset.Zero },
                        onDragEnd = { onDragEnd(dragDelta) },
                        onDragCancel = { dragDelta = Offset.Zero },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDelta += dragAmount
                        }
                    )
                }
            }
            .pointerTap(disabled) { onTap() }
    ) {}
}