package com.byteflipper.random.ui.ball.gl

import com.byteflipper.random.ui.gl.Mesh
import com.byteflipper.random.ui.gl.VertexAttribute
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Geometry the ball needs. Every factory has to be called on the GL thread. */
object MeshFactory {

    /** Screen-filling quad in clip space, used by the backdrop and the interior pass. */
    fun fullscreenQuad(): Mesh = Mesh(
        vertices = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            1f, 1f,
            -1f, 1f
        ),
        indices = shortArrayOf(0, 1, 2, 0, 2, 3),
        attributes = listOf(VertexAttribute("aPosition", 2))
    )

    /**
     * Unit UV sphere. Only positions are stored: on a unit sphere the position *is* the normal, and
     * halving the vertex data keeps the upload cheap.
     */
    fun uvSphere(segments: Int = 48, rings: Int = 32): Mesh {
        require(segments >= 3 && rings >= 2) { "A sphere needs at least 3 segments and 2 rings" }

        val vertices = FloatArray((segments + 1) * (rings + 1) * 3)
        var v = 0
        for (ring in 0..rings) {
            val phi = PI * ring / rings
            val y = cos(phi).toFloat()
            val ringRadius = sin(phi).toFloat()
            for (segment in 0..segments) {
                val theta = 2.0 * PI * segment / segments
                vertices[v++] = ringRadius * sin(theta).toFloat()
                vertices[v++] = y
                vertices[v++] = ringRadius * cos(theta).toFloat()
            }
        }

        val indices = ShortArray(segments * rings * 6)
        var i = 0
        for (ring in 0 until rings) {
            for (segment in 0 until segments) {
                val current = ring * (segments + 1) + segment
                val next = current + segments + 1
                // Counter-clockwise seen from outside, so back-face culling keeps the far side out.
                indices[i++] = current.toShort()
                indices[i++] = next.toShort()
                indices[i++] = (current + 1).toShort()
                indices[i++] = (current + 1).toShort()
                indices[i++] = next.toShort()
                indices[i++] = (next + 1).toShort()
            }
        }

        return Mesh(
            vertices = vertices,
            indices = indices,
            attributes = listOf(VertexAttribute("aPosition", 3))
        )
    }
}
