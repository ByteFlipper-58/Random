package com.byteflipper.random.ui.ball.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.byteflipper.random.R
import kotlin.math.log2
import kotlin.math.max

/**
 * The answers, drawn into a grid of square cells and handed to the interior shader as one texture:
 * cell *i* is the text that face *i* of the die carries.
 *
 * Only the alpha channel is read — the ink colour is a uniform — so the cells hold white text on
 * transparent. Mipmaps matter here: a face is about sixty pixels across on screen against a cell of
 * [CELL_PIXELS], and without them the text would crawl as the die turns.
 *
 * Built and used on the GL thread only. Rebuilding costs twenty text layouts and one upload, which is
 * why it only happens when the answers or the locale actually change.
 *
 * [unit] is the texture unit the sheet lives on for good: the density grid has one of its own, and
 * uploading onto whichever unit happened to be active would tangle the two.
 */
class TextAtlas(private val context: Context, private val unit: Int) {

    private val handles = IntArray(1)

    /** What is on the GPU right now, so pushing the same labels again costs one list comparison. */
    private var uploaded: List<String> = emptyList()

    /** False while every label is blank; the die then shows plain faces instead of empty cells. */
    private var hasInk = false

    val cellsAcross: Int get() = CELLS_ACROSS
    val cellsDown: Int get() = CELLS_DOWN

    val isReady: Boolean get() = handles[0] != 0 && hasInk

    /**
     * Redraws the sheet for [labels], one per die face, and uploads it. A repeated call with the same
     * text does nothing.
     */
    fun setLabels(labels: List<String>) {
        if (labels == uploaded) return
        uploaded = labels.toList()
        hasInk = uploaded.any { it.isNotBlank() }
        if (!hasInk) return

        val bitmap = draw(uploaded)
        bindForUpload()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bitmap.recycle()
    }

    /** Makes the sheet current on its own texture unit, ready for the interior program to sample. */
    fun bind() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handles[0])
    }

    /**
     * Mip level to sample a face at, from how many screen pixels the text square covers.
     *
     * The interior shader picks this up as a uniform because the ray-march reaches the die inside
     * divergent control flow, where implicit derivatives — and so automatic mip selection — are not
     * to be trusted.
     */
    fun lodFor(textPixels: Float): Float =
        max(0f, log2(CELL_PIXELS.toFloat() / max(textPixels, 1f)))

    fun release() {
        if (handles[0] == 0) return
        GLES30.glDeleteTextures(1, handles, 0)
        handles[0] = 0
        uploaded = emptyList()
        hasInk = false
    }

    private fun bindForUpload() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        if (handles[0] == 0) {
            GLES30.glGenTextures(1, handles, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handles[0])
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR_MIPMAP_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            // Cells sit side by side, so a wrapped sample would read a neighbour's words.
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE
            )
        } else {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handles[0])
        }
    }

    private fun draw(labels: List<String>): Bitmap {
        val bitmap = createBitmap(CELLS_ACROSS * CELL_PIXELS, CELLS_DOWN * CELL_PIXELS)
        val canvas = Canvas(bitmap)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = answerTypeface()
            color = Color.WHITE
        }

        for (cell in 0 until CELLS_ACROSS * CELLS_DOWN) {
            val text = labels.getOrNull(cell)?.trim().orEmpty()
            if (text.isEmpty()) continue

            val layout = fit(text, paint)
            val left = (cell % CELLS_ACROSS) * CELL_PIXELS
            val top = (cell / CELLS_ACROSS) * CELL_PIXELS

            // The clip is the guarantee: however long an answer someone types, it cannot leak into
            // the neighbouring face.
            canvas.withClip(
                (left + MARGIN_PIXELS).toFloat(),
                (top + MARGIN_PIXELS).toFloat(),
                (left + CELL_PIXELS - MARGIN_PIXELS).toFloat(),
                (top + CELL_PIXELS - MARGIN_PIXELS).toFloat()
            ) {
                translate(
                    (left + MARGIN_PIXELS).toFloat(),
                    top + (CELL_PIXELS - layout.height) / 2f
                )
                layout.draw(this)
            }
        }
        return bitmap
    }

    /**
     * Largest layout of [text] that still fits a cell's inner box.
     *
     * The size is searched rather than fixed because the set runs from "Yes" to a whole sentence: a
     * short answer should fill its face, a long one shrink until it is all there.
     */
    private fun fit(text: String, paint: TextPaint): StaticLayout {
        val box = CELL_PIXELS - 2 * MARGIN_PIXELS
        var size = MAX_TEXT_PIXELS
        var layout = layoutAt(text, paint, size, box)
        while (layout.height > box && size > MIN_TEXT_PIXELS) {
            size = max(size * SHRINK_STEP, MIN_TEXT_PIXELS)
            layout = layoutAt(text, paint, size, box)
        }
        return layout
    }

    private fun layoutAt(text: String, paint: TextPaint, size: Float, width: Int): StaticLayout {
        paint.textSize = size
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, LINE_SPACING)
            .build()
    }

    /**
     * The app's own rounded face, in bold for legibility at this size.
     *
     * Deliberately the bundled file rather than the downloadable Montserrat the headings use: this
     * runs on the GL thread, where a round trip to the font provider is not an option.
     */
    private fun answerTypeface(): Typeface {
        val base = runCatching {
            ResourcesCompat.getFont(context, R.font.google_sans_rounded_regular)
        }.getOrNull() ?: Typeface.SANS_SERIF
        return Typeface.create(base, Typeface.BOLD)
    }

    private companion object {
        /** Twenty faces fit a 5x4 sheet exactly. */
        const val CELLS_ACROSS = 5
        const val CELLS_DOWN = 4

        /**
         * Side of one cell. Roughly twice what a face covers on screen even on a tablet, so the top
         * mip is never magnified.
         */
        const val CELL_PIXELS = 192

        /** Transparent border, so a coarse mip cannot smear one face's text onto another. */
        const val MARGIN_PIXELS = 14

        const val MAX_TEXT_PIXELS = 46f
        const val MIN_TEXT_PIXELS = 13f
        const val SHRINK_STEP = 0.88f
        const val LINE_SPACING = 0.95f
    }
}
