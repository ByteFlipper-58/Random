package com.byteflipper.random.ui.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One interleaved vertex attribute: how many floats it takes and what it is called in the shader. */
data class VertexAttribute(val name: String, val components: Int)

/**
 * An indexed triangle mesh living in a VBO/IBO pair.
 *
 * Constructed on the GL thread; [release] has to run there too.
 */
class Mesh(
    vertices: FloatArray,
    indices: ShortArray,
    private val attributes: List<VertexAttribute>
) {

    private val buffers = IntArray(2)
    private val indexCount = indices.size
    private val strideBytes = attributes.sumOf { it.components } * FLOAT_BYTES

    init {
        GLES30.glGenBuffers(2, buffers, 0)

        val vertexData = ByteBuffer
            .allocateDirect(vertices.size * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0])
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * FLOAT_BYTES,
            vertexData,
            GLES30.GL_STATIC_DRAW
        )

        val indexData = ByteBuffer
            .allocateDirect(indices.size * SHORT_BYTES)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
            .position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[1])
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * SHORT_BYTES,
            indexData,
            GLES30.GL_STATIC_DRAW
        )

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun draw(program: GlProgram) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0])
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[1])

        var offsetBytes = 0
        val enabled = IntArray(attributes.size)
        attributes.forEachIndexed { index, attribute ->
            val location = program.attribute(attribute.name)
            enabled[index] = location
            if (location >= 0) {
                GLES30.glEnableVertexAttribArray(location)
                GLES30.glVertexAttribPointer(
                    location,
                    attribute.components,
                    GLES30.GL_FLOAT,
                    false,
                    strideBytes,
                    offsetBytes
                )
            }
            offsetBytes += attribute.components * FLOAT_BYTES
        }

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)

        enabled.forEach { location ->
            if (location >= 0) GLES30.glDisableVertexAttribArray(location)
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun release() {
        GLES30.glDeleteBuffers(2, buffers, 0)
    }

    private companion object {
        const val FLOAT_BYTES = 4
        const val SHORT_BYTES = 2
    }
}
