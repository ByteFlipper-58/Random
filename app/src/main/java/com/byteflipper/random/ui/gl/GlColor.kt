package com.byteflipper.random.ui.gl

import androidx.compose.ui.graphics.Color

/** A theme colour as the three floats a shader uniform takes. Alpha is dropped; nothing here is glass. */
fun Color.toRgb(): FloatArray = floatArrayOf(red, green, blue)

/** The whole list packed end to end, three floats a colour, in the order it was given in. */
fun List<Color>.toRgb(): FloatArray {
    val packed = FloatArray(size * 3)
    forEachIndexed { index, color ->
        packed[index * 3] = color.red
        packed[index * 3 + 1] = color.green
        packed[index * 3 + 2] = color.blue
    }
    return packed
}
