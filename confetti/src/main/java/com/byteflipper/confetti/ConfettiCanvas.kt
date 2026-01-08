package com.byteflipper.confetti

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas для отрисовки частиц конфетти.
 * 
 * @param particles Список частиц для отрисовки
 * @param modifier Modifier для Canvas
 */
@Composable
internal fun ConfettiCanvas(
    particles: List<ConfettiParticle>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            drawParticle(particle)
        }
    }
}

/**
 * Отрисовывает одну частицу.
 */
private fun DrawScope.drawParticle(particle: ConfettiParticle) {
    val color = particle.color.copy(alpha = particle.alpha)
    
    rotate(
        degrees = particle.rotation,
        pivot = Offset(particle.x, particle.y)
    ) {
        when (particle.shape) {
            ConfettiShape.RECTANGLE -> {
                drawRect(
                    color = color,
                    topLeft = Offset(
                        particle.x - particle.size / 2,
                        particle.y - particle.size / 4
                    ),
                    size = Size(particle.size, particle.size / 2)
                )
            }
            
            ConfettiShape.CIRCLE -> {
                drawCircle(
                    color = color,
                    radius = particle.size / 2,
                    center = Offset(particle.x, particle.y)
                )
            }
            
            ConfettiShape.TRIANGLE -> {
                val path = Path().apply {
                    val halfSize = particle.size / 2
                    moveTo(particle.x, particle.y - halfSize)
                    lineTo(particle.x - halfSize, particle.y + halfSize)
                    lineTo(particle.x + halfSize, particle.y + halfSize)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}
