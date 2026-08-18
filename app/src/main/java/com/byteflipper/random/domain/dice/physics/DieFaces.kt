package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Vec3

/**
 * The six sides of a d6 in one place, so the simulation, the guidance that brings a number up, and
 * the mesh the renderer builds all agree on which side carries which number.
 *
 * The arrangement is the western right-handed one: with the 1 up and the 2 facing the player, the 3
 * sits on the right. Opposite faces summing to seven falls out of that rather than being imposed on
 * it — which is worth having, because a die that gets this wrong looks wrong to anyone who has ever
 * held one.
 */
object DieFaces {

    const val FACE_COUNT = 6

    const val MIN_VALUE = 1
    const val MAX_VALUE = 6

    /** Outward face normals in the die's own frame; unit vectors along the body axes. */
    val normals: List<Vec3> = listOf(
        Vec3(1f, 0f, 0f),
        Vec3(-1f, 0f, 0f),
        Vec3(0f, 1f, 0f),
        Vec3(0f, -1f, 0f),
        Vec3(0f, 0f, 1f),
        Vec3(0f, 0f, -1f)
    )

    /** The number printed on each face, in the same order as [normals]. */
    val values: IntArray = intArrayOf(3, 4, 1, 6, 2, 5)

    /** The face carrying [value]; out-of-range values fall back to the one showing a 1. */
    fun faceOf(value: Int): Int {
        val face = values.indexOf(value)
        return if (face >= 0) face else values.indexOf(MIN_VALUE)
    }

    /** Body-frame normal of the face carrying [value]. */
    fun normalOf(value: Int): Vec3 = normals[faceOf(value)]
}
