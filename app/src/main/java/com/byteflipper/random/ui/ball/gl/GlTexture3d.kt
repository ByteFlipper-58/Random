package com.byteflipper.random.ui.ball.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A cubic single-channel 3D texture holding the liquid's density, re-uploaded every frame.
 *
 * At 32³ the grid is 32 KB, so uploading the whole thing costs less than any scheme for uploading
 * part of it would. Storage is allocated once and never resized.
 *
 * Constructed on the GL thread; every method has to run there too.
 */
class GlTexture3d(private val resolution: Int) {

    private val handles = IntArray(1)
    private val buffer: ByteBuffer = ByteBuffer
        .allocateDirect(resolution * resolution * resolution)
        .order(ByteOrder.nativeOrder())

    init {
        GLES30.glGenTextures(1, handles, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, handles[0])
        GLES30.glTexStorage3D(
            GLES30.GL_TEXTURE_3D,
            1,
            GLES30.GL_R8,
            resolution,
            resolution,
            resolution
        )
        // Trilinear filtering is what turns a coarse grid into a smooth surface; no mips exist.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_3D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_3D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_3D,
            GLES30.GL_TEXTURE_WRAP_R,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
    }

    /** Replaces the whole volume. [data] has to be `resolution³` bytes, x fastest and z slowest. */
    fun upload(data: ByteArray) {
        val expected = resolution * resolution * resolution
        if (data.size < expected) return

        buffer.clear()
        buffer.put(data, 0, expected)
        buffer.position(0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, handles[0])
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexSubImage3D(
            GLES30.GL_TEXTURE_3D,
            0,
            0,
            0,
            0,
            resolution,
            resolution,
            resolution,
            GLES30.GL_RED,
            GLES30.GL_UNSIGNED_BYTE,
            buffer
        )
    }

    fun bind(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, handles[0])
    }

    fun release() {
        GLES30.glDeleteTextures(1, handles, 0)
        handles[0] = 0
    }
}
