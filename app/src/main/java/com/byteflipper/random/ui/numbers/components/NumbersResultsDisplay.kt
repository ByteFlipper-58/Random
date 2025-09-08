package com.byteflipper.random.ui.numbers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R

private fun getContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun NumbersResultsDisplay(
    results: List<Int>,
    cardColor: Color,
    cardSize: Dp
) {
    if (results.isNotEmpty()) {
        val lazyListState = rememberLazyListState()
        val textColor = getContrastColor(cardColor)

        // Адаптивный размер шрифта - более компактный
        fun numberFontSizeFor(count: Int, size: Dp): Float {
            val k = when {
                count <= 10 -> 0.06f
                count <= 25 -> 0.04f
                count <= 50 -> 0.03f
                count <= 100 -> 0.025f
                else -> 0.02f
            }
            return (size.value * k).coerceIn(12f, 28f)
        }

        // Максимальная высота - 90% от карточки для большего пространства
        val maxHeight = (cardSize * 0.9f).coerceAtLeast(200.dp)

        AnimatedVisibility(
            visible = true,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp) // Уменьшили отступы
                    .heightIn(max = maxHeight)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = lazyListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                ) {
                    val fontSize = numberFontSizeFor(results.size, cardSize).sp

                    // Более компактное группирование
                    val chunkSize = when {
                        results.size <= 20 -> results.size // Одна строка для малых количеств
                        results.size <= 50 -> (results.size + 1) / 2
                        results.size <= 100 -> (results.size + 2) / 3
                        else -> (results.size + 4) / 5
                    }
                    val lines = results.chunked(chunkSize)

                    itemsIndexed(lines) { index, lineNumbers ->
                        val line = lineNumbers.joinToString(", ")
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                fontSize = fontSize,
                                lineHeight = fontSize * 1.1f // Уменьшенный межстрочный интервал
                            ),
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp) // Минимальные отступы
                        )
                    }
                }
            }
        }
    }
}
