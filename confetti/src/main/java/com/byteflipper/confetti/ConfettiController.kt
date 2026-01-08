package com.byteflipper.confetti

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Контроллер для управления анимацией конфетти.
 */
@Stable
class ConfettiController {
    
    private val _particles = mutableStateListOf<ConfettiParticle>()
    val particles: List<ConfettiParticle> get() = _particles
    
    var isActive by mutableStateOf(false)
        private set
    
    var progress by mutableFloatStateOf(0f)
        private set
    
    private var config: ConfettiConfig = ConfettiConfig()
    private var startTime: Long = 0L
    private var duration: Long = 3000L
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    
    /**
     * Запускает анимацию конфетти.
     */
    fun start(
        centerX: Float,
        centerY: Float,
        config: ConfettiConfig = ConfettiConfig(),
        durationMs: Long = 3000L,
        screenWidth: Float,
        screenHeight: Float
    ) {
        this.config = config
        this.duration = durationMs
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.startTime = System.currentTimeMillis()
        
        _particles.clear()
        repeat(config.particleCount) {
            _particles.add(createParticle(centerX, centerY, screenWidth, screenHeight, config))
        }
        
        isActive = true
        progress = 0f
    }
    
    /**
     * Обновляет состояние всех частиц.
     */
    fun update(): Boolean {
        if (!isActive) return false
        
        val elapsed = System.currentTimeMillis() - startTime
        progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        
        if (elapsed >= duration) {
            stop()
            return false
        }
        
        // Вычисляем alpha для затухания
        val fadeStart = 1f - config.fadeOutDuration
        val fadeAlpha = if (progress > fadeStart) {
            1f - (progress - fadeStart) / config.fadeOutDuration
        } else {
            1f
        }
        
        // Обновляем каждую частицу
        for (particle in _particles) {
            particle.velocityY += config.gravity
            particle.velocityX *= (1f - config.drag)
            particle.velocityY *= (1f - config.drag)
            
            particle.x += particle.velocityX
            particle.y += particle.velocityY
            particle.rotation += particle.rotationSpeed
            particle.alpha = fadeAlpha
        }
        
        return true
    }
    
    /**
     * Останавливает анимацию и очищает частицы.
     */
    fun stop() {
        isActive = false
        _particles.clear()
        progress = 0f
    }
}
