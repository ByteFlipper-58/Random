package com.byteflipper.confetti

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Модель частицы конфетти.
 */
/**
 * Модель частицы конфетти.
 * Используем mutableFloatStateOf для свойств, изменение которых должно вызывать перерисовку.
 */
class ConfettiParticle(
    initialX: Float,
    initialY: Float,
    var velocityX: Float,
    var velocityY: Float,
    initialRotation: Float,
    var rotationSpeed: Float,
    val color: Color,
    val size: Float,
    initialAlpha: Float = 1f,
    val shape: ConfettiShape = ConfettiShape.RECTANGLE
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var rotation by mutableFloatStateOf(initialRotation)
    var alpha by mutableFloatStateOf(initialAlpha)
}

/**
 * Формы конфетти.
 */
enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    TRIANGLE
}

/**
 * Режимы анимации конфетти.
 */
enum class ConfettiMode {
    /** Боковые пушки — стреляет с обоих нижних углов вверх по диагонали */
    SIDE_CANNONS,

    /** Фонтан/Взрыв — вылетает из центра вверх веером */
    BLAST
}

/**
 * Конфигурация для генерации конфетти.
 */
data class ConfettiConfig(
    val colors: List<Color> = defaultConfettiColors(),
    val particleCount: Int = 100,
    val minSize: Float = 8f,
    val maxSize: Float = 16f,
    val minSpeed: Float = 8f,
    val maxSpeed: Float = 25f,
    val gravity: Float = 0.4f,
    val drag: Float = 0.01f,
    val fadeOutDuration: Float = 0.3f,
    val mode: ConfettiMode = ConfettiMode.SIDE_CANNONS
)

/**
 * Возвращает список цветов по умолчанию для конфетти.
 */
fun defaultConfettiColors(): List<Color> = listOf(
    Color(0xFFFF6B6B), // красный
    Color(0xFFFFE66D), // жёлтый
    Color(0xFF4ECDC4), // бирюзовый
    Color(0xFF45B7D1), // голубой
    Color(0xFFFF8ED4), // розовый
    Color(0xFFA855F7), // фиолетовый
    Color(0xFF6366F1), // индиго
    Color(0xFF22C55E), // зелёный
    Color(0xFFF97316)  // оранжевый
)

/**
 * Создаёт частицу в зависимости от выбранного режима.
 */
internal fun createParticle(
    centerX: Float,
    centerY: Float,
    screenWidth: Float,
    screenHeight: Float,
    config: ConfettiConfig
): ConfettiParticle {
    val random = Random
    
    return when (config.mode) {
        ConfettiMode.SIDE_CANNONS -> createSideCannonsParticle(screenWidth, screenHeight, config, random)
        ConfettiMode.BLAST -> createBlastParticle(centerX, centerY, config, random)
    }
}



/** Боковые пушки — стреляет с нижних углов вверх по диагонали */
private fun createSideCannonsParticle(
    screenWidth: Float,
    screenHeight: Float,
    config: ConfettiConfig,
    random: Random
): ConfettiParticle {
    // Выбираем сторону: левая или правая
    val isLeft = random.nextBoolean()
    val startX = if (isLeft) 0f else screenWidth
    val startY = screenHeight * 0.45f
    
    // Угол: вверх и к центру
    val baseAngle = if (isLeft) -60f else -120f // Градусы (вверх-вправо или вверх-влево)
    val angleVariation = random.nextFloat() * 60f - 30f
    val angle = (baseAngle + angleVariation) * (PI.toFloat() / 180f)
    
    val speed = random.nextFloat() * (config.maxSpeed - config.minSpeed) + config.minSpeed
    
    return ConfettiParticle(
        initialX = startX,
        initialY = startY,
        velocityX = cos(angle) * speed,
        velocityY = sin(angle) * speed,
        initialRotation = random.nextFloat() * 360f,
        rotationSpeed = random.nextFloat() * 20f - 10f,
        color = config.colors.random(),
        size = random.nextFloat() * (config.maxSize - config.minSize) + config.minSize,
        shape = ConfettiShape.entries.random()
    )
}

/** Фонтан — вылетает из центра вверх конусом */
private fun createBlastParticle(
    centerX: Float,
    centerY: Float,
    config: ConfettiConfig,
    random: Random
): ConfettiParticle {
    // Угол: конус вверх (от -150° до -30° = от 210° до 330° в радианах?)
    // 0 - вправо, -90 - вверх, 180 - влево, 90 - вниз.
    // Нам нужно вокруг -90. Например от -135 (-3pi/4) до -45 (-pi/4).
    val minAngle = -135f * (PI.toFloat() / 180f)
    val maxAngle = -45f * (PI.toFloat() / 180f)
    val angle = random.nextFloat() * (maxAngle - minAngle) + minAngle
    
    // Скорость побольше, чтобы высоко летело
    val speed = (random.nextFloat() * (config.maxSpeed - config.minSpeed) + config.minSpeed) * 1.5f
    
    return ConfettiParticle(
        initialX = centerX,
        initialY = centerY,
        velocityX = cos(angle) * speed,
        velocityY = sin(angle) * speed, // Вверх
        initialRotation = random.nextFloat() * 360f,
        rotationSpeed = random.nextFloat() * 30f - 15f,
        color = config.colors.random(),
        size = random.nextFloat() * (config.maxSize - config.minSize) + config.minSize,
        shape = ConfettiShape.entries.random()
    )
}
