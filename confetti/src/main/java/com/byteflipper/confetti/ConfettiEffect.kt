package com.byteflipper.confetti

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Composable для отображения эффекта конфетти.
 * 
 * Использование:
 * ```kotlin
 * var showConfetti by remember { mutableStateOf(false) }
 * 
 * ConfettiEffect(
 *     trigger = showConfetti,
 *     mode = ConfettiMode.TOP_SHOWER,
 *     onComplete = { showConfetti = false }
 * )
 * ```
 * 
 * @param trigger Когда становится true — запускается анимация
 * @param mode Режим анимации (TOP_SHOWER, EXPLOSION, FIREWORK, SIDE_CANNONS)
 * @param colors Список цветов для частиц
 * @param particleCount Количество частиц
 * @param durationMs Длительность анимации в миллисекундах
 * @param modifier Modifier для контейнера
 * @param onComplete Callback при завершении анимации
 */
@Composable
fun ConfettiEffect(
    trigger: Boolean,
    mode: ConfettiMode = ConfettiMode.SIDE_CANNONS,
    colors: List<Color> = defaultConfettiColors(),
    particleCount: Int = 150,
    durationMs: Long = 3000L,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val controller = remember { ConfettiController() }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
                containerHeight = coordinates.size.height.toFloat()
            }
    ) {
        // Запуск анимации при trigger
        LaunchedEffect(trigger) {
            if (trigger && containerWidth > 0 && containerHeight > 0) {
                val centerX = containerWidth / 2
                val centerY = containerHeight / 2
                
                controller.start(
                    centerX = centerX,
                    centerY = centerY,
                    config = ConfettiConfig(
                        colors = colors,
                        particleCount = particleCount,
                        mode = mode
                    ),
                    durationMs = durationMs,
                    screenWidth = containerWidth,
                    screenHeight = containerHeight
                )
            }
        }
        
        // Цикл анимации
        LaunchedEffect(controller.isActive) {
            if (controller.isActive) {
                while (controller.isActive) {
                    withFrameMillis {
                        if (!controller.update()) {
                            onComplete()
                        }
                    }
                }
            }
        }
        
        // Отрисовка
        if (controller.isActive) {
            ConfettiCanvas(
                particles = controller.particles,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Запоминает контроллер конфетти для ручного управления.
 */
@Composable
fun rememberConfettiController(): ConfettiController {
    return remember { ConfettiController() }
}
