package com.byteflipper.random.ui.numbers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

        // Вычисляем максимальную длину числа для адаптации размера
        val maxDigits = results.maxOfOrNull { it.toString().length } ?: 1
        
        fun numberFontSizeFor(count: Int, size: Dp, maxLen: Int): Float {
            // Базовый коэффициент зависит от количества элементов
            val countK = when {
                count == 1 -> 0.28f  // Одно число - очень крупно
                count <= 3 -> 0.20f  // 2-3 числа - крупно
                count <= 5 -> 0.14f  // 4-5 чисел - крупнее среднего
                count <= 10 -> 0.09f
                count <= 25 -> 0.05f
                count <= 50 -> 0.035f
                count <= 100 -> 0.025f
                else -> 0.02f
            }
            // Корректируем на длину числа: длинные числа требуют меньший шрифт
            val lenAdjust = when {
                maxLen <= 2 -> 1.0f  // 1-99
                maxLen <= 4 -> 0.85f  // 100-9999
                maxLen <= 6 -> 0.7f   // 10000-999999
                else -> 0.55f
            }
            return (size.value * countK * lenAdjust).coerceIn(14f, 72f)
        }

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
                    .padding(16.dp)
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
                    val fontSize = numberFontSizeFor(results.size, cardSize, maxDigits).sp

                    val chunkSize = when {
                        results.size <= 20 -> results.size
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
                                lineHeight = fontSize * 1.1f
                            ),
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


