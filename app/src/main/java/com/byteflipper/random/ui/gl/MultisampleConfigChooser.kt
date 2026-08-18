package com.byteflipper.random.ui.gl

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/** Picks a 4x multisampled ES3 config, falling back to a plain depth-buffered surface. */
class MultisampleConfigChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig =
        choose(egl, display, samples = 4)
            ?: choose(egl, display, samples = 0)
            ?: error("No suitable EGL config for an ES3 surface")

    private fun choose(egl: EGL10, display: EGLDisplay, samples: Int): EGLConfig? {
        val attributes = buildList {
            addAll(listOf(EGL10.EGL_RED_SIZE, 8))
            addAll(listOf(EGL10.EGL_GREEN_SIZE, 8))
            addAll(listOf(EGL10.EGL_BLUE_SIZE, 8))
            addAll(listOf(EGL10.EGL_ALPHA_SIZE, 0))
            addAll(listOf(EGL10.EGL_DEPTH_SIZE, 16))
            addAll(listOf(EGL10.EGL_STENCIL_SIZE, 0))
            addAll(listOf(EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT))
            if (samples > 0) {
                addAll(listOf(EGL10.EGL_SAMPLE_BUFFERS, 1))
                addAll(listOf(EGL10.EGL_SAMPLES, samples))
            }
            add(EGL10.EGL_NONE)
        }.toIntArray()

        val count = IntArray(1)
        if (!egl.eglChooseConfig(display, attributes, null, 0, count) || count[0] <= 0) return null

        val configs = arrayOfNulls<EGLConfig>(count[0])
        if (!egl.eglChooseConfig(display, attributes, configs, count[0], count)) return null
        return configs.firstOrNull()
    }

    private companion object {
        const val EGL_OPENGL_ES3_BIT = 0x0040
    }
}
