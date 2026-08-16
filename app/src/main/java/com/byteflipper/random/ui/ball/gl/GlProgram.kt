package com.byteflipper.random.ui.ball.gl

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A linked shader program with cached uniform and attribute locations.
 *
 * Every method has to be called on the GL thread; nothing here is thread-safe by design.
 */
class GlProgram private constructor(private val handle: Int) {

    private val locations = HashMap<String, Int>()

    fun use() {
        GLES30.glUseProgram(handle)
    }

    /** Location of a uniform, or -1 when the compiler optimised it away. */
    fun uniform(name: String): Int = locations.getOrPut("u:$name") {
        GLES30.glGetUniformLocation(handle, name)
    }

    fun attribute(name: String): Int = locations.getOrPut("a:$name") {
        GLES30.glGetAttribLocation(handle, name)
    }

    fun release() {
        GLES30.glDeleteProgram(handle)
        locations.clear()
    }

    companion object {
        private const val TAG = "GlProgram"

        /** Where linked binaries are kept, under the app's own cache. */
        private const val CACHE_DIRECTORY = "gl_programs"

        /**
         * Builds a program out of two `res/raw` GLSL files, from a cached binary when there is one.
         *
         * The interior shader takes a driver a second or two to compile the first time, and that wait
         * was the wait before the ball appeared. A linked binary is the same program without the
         * compile, so every open after the first skips it — and because the key covers both sources
         * *and* the driver's own name and version, an edited shader or an updated driver simply misses
         * and compiles again.
         */
        fun fromRaw(context: Context, vertexRes: Int, fragmentRes: Int): GlProgram {
            val vertexSource = readRaw(context, vertexRes)
            val fragmentSource = readRaw(context, fragmentRes)
            val cache = cacheFile(context, vertexSource, fragmentSource)

            cache?.let(::loadBinary)?.let { return GlProgram(it) }

            val program = link(vertexSource, fragmentSource)
            cache?.let { file -> saveBinary(file, program) }
            return GlProgram(program)
        }

        private fun link(vertexSource: String, fragmentSource: String): Int {
            val vertex = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
            val fragment = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertex)
            GLES30.glAttachShader(program, fragment)
            // Asked for before the link, because a driver is allowed to throw the binary away
            // otherwise — and then there would be nothing to cache.
            GLES30.glProgramParameteri(
                program,
                GLES30.GL_PROGRAM_BINARY_RETRIEVABLE_HINT,
                GLES30.GL_TRUE
            )
            GLES30.glLinkProgram(program)

            // The shaders are only needed until the program is linked.
            GLES30.glDetachShader(program, vertex)
            GLES30.glDetachShader(program, fragment)
            GLES30.glDeleteShader(vertex)
            GLES30.glDeleteShader(fragment)

            val status = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                Log.e(TAG, "Program link failed: $log")
                error("Program link failed: $log")
            }

            return program
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)

            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                Log.e(TAG, "Shader compile failed: $log")
                error("Shader compile failed: $log")
            }
            return shader
        }

        /**
         * Where this pair of sources is cached for this driver, or null when there is nowhere to put
         * it. The driver's name and version are part of the key because a binary is only valid for the
         * driver that produced it.
         */
        private fun cacheFile(
            context: Context,
            vertexSource: String,
            fragmentSource: String
        ): File? {
            val directory = File(context.cacheDir, CACHE_DIRECTORY)
            if (!directory.isDirectory && !directory.mkdirs()) return null

            val driver = buildString {
                append(GLES30.glGetString(GLES30.GL_RENDERER).orEmpty())
                append(GLES30.glGetString(GLES30.GL_VERSION).orEmpty())
            }
            val name = listOf(vertexSource, fragmentSource, driver)
                .joinToString(separator = "-") { part -> Integer.toHexString(part.hashCode()) }

            return File(directory, "$name.bin")
        }

        /** The cached program, or null when there is none, it is unreadable, or the driver refuses it. */
        private fun loadBinary(file: File): Int? {
            if (!file.isFile) return null

            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
            if (bytes.size <= Int.SIZE_BYTES) return null

            val payload = ByteBuffer.allocateDirect(bytes.size - Int.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .put(bytes, Int.SIZE_BYTES, bytes.size - Int.SIZE_BYTES)
            payload.rewind()

            val format = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).int
            val program = GLES30.glCreateProgram()
            drainErrors()
            GLES30.glProgramBinary(program, format, payload, payload.remaining())

            // A refused binary is reported as a failed link, and the format itself can be rejected
            // outright — either way there is a source compile to fall back on.
            val status = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
            val failed = drainErrors() || status[0] == 0
            if (failed) {
                GLES30.glDeleteProgram(program)
                file.delete()
                return null
            }

            return program
        }

        /** Stores the linked binary, so that the next launch has a compile to skip. */
        private fun saveBinary(file: File, program: Int) {
            val length = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_PROGRAM_BINARY_LENGTH, length, 0)
            if (length[0] <= 0) return

            val buffer = ByteBuffer.allocateDirect(length[0]).order(ByteOrder.nativeOrder())
            val written = IntArray(1)
            val format = IntArray(1)
            drainErrors()
            GLES30.glGetProgramBinary(program, length[0], written, 0, format, 0, buffer)
            if (drainErrors() || written[0] <= 0) return

            val bytes = ByteArray(written[0])
            buffer.rewind()
            buffer.get(bytes)

            val header = ByteBuffer.allocate(Int.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .putInt(format[0])
                .array()

            // Written beside the target and moved into place, so a kill mid-write cannot leave a
            // truncated binary behind for the next launch to trust. Inline: a few hundred kilobytes
            // straight after a compile that cost hundreds of milliseconds is not worth a thread.
            val temporary = File(file.parentFile, "${file.name}.tmp")
            val stored = runCatching {
                temporary.outputStream().buffered().use { out ->
                    out.write(header)
                    out.write(bytes)
                }
                temporary.renameTo(file)
            }.getOrDefault(false)
            if (!stored) temporary.delete()
        }

        /** Empties the error queue, reporting whether anything was in it. */
        private fun drainErrors(): Boolean {
            var found = false
            while (GLES30.glGetError() != GLES30.GL_NO_ERROR) found = true
            return found
        }

        private fun readRaw(context: Context, resId: Int): String =
            context.resources.openRawResource(resId).use { stream ->
                stream.reader().readText()
            }
    }
}
