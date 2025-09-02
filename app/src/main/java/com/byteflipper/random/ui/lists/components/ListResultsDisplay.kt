package com.byteflipper.random.ui.lists.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.utils.Constants

private fun getContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun ListResultsDisplay(
    results: List<String>,
    cardColor: Color,
    cardSize: Dp
) {
    if (results.isNotEmpty()) {
        val lazyListState = rememberLazyListState()
        // Уменьшенные отступы для компактности
        val adaptivePadding = 16.dp
        val adaptiveSpacing = 4.dp

        AnimatedVisibility(
            visible = true,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(adaptivePadding)
                    .heightIn(max = (cardSize * 0.9f).coerceAtLeast(200.dp))
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = lazyListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val itemTextColor = getContrastColor(cardColor)

                    // Более эффективная группировка для списков
                    val chunkSize = when {
                        results.size <= 5 -> results.size  // Одна строка
                        results.size <= 15 -> (results.size + 1) / 2
                        results.size <= 30 -> (results.size + 2) / 3
                        else -> (results.size + 3) / 4
                    }
                    val lines = results.chunked(chunkSize)

                    items(lines) { lineItems ->
                        val line = lineItems.joinToString(", ")
                        // Адаптивный размер шрифта в зависимости от количества элементов
                        val fontSize = when {
                            results.size <= 10 -> 18.sp
                            results.size <= 25 -> 16.sp
                            results.size <= 50 -> 14.sp
                            else -> 12.sp
                        }
                        
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                fontSize = fontSize,
                                lineHeight = fontSize * 1.1f // Компактный межстрочный интервал
                            ),
                            color = itemTextColor,
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
