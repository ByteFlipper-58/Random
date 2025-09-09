package com.byteflipper.random.ui.lot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

@Composable
fun LotGrid(
    modifier: Modifier = Modifier,
    cards: List<LotCard>,
    onCardClick: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val spacing = 8.dp
        val rows = computeRowSizes(cards.size)
        val maxInRow = rows.maxOrNull() ?: 1
        val widthCandidate = (maxWidth - spacing * (maxInRow - 1)) / maxInRow
        val heightCandidate = (maxHeight - spacing * (rows.size - 1)) / rows.size
        val cardSize = min(widthCandidate.value, heightCandidate.value).dp
            .coerceIn(40.dp, 180.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var idx = 0
            rows.forEachIndexed { rowIndex, countInRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = spacing,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(countInRow) { colInRow ->
                        val card = cards[idx]
                        // вычисляем задержку появления от центра
                        val centerCol = (countInRow - 1) / 2f
                        val dist = abs(colInRow - centerCol)
                        val centerRow = (rows.size - 1) / 2f
                        val rowDist = abs(rowIndex - centerRow)
                        val baseDelay = 30
                        val appearDelayMs = (rowDist * 55 + dist * 45 + baseDelay).toInt()
                        LotGridCard(
                            modifier = Modifier.size(cardSize),
                            isRevealed = card.isRevealed,
                            isMarked = card.isMarked,
                            cardColor = card.color,
                            cardSize = cardSize,
                            onClick = { onCardClick(card.id) },
                            appearDelayMs = appearDelayMs
                        )
                        idx++
                    }
                }
                if (rowIndex < rows.size - 1) {
                    Spacer(Modifier.height(spacing))
                }
            }
        }
    }
}


