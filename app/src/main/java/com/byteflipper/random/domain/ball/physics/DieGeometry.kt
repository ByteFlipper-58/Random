package com.byteflipper.random.domain.ball.physics

import com.byteflipper.random.domain.physics.Vec3
import kotlin.math.sqrt

/**
 * The answer die: a regular icosahedron, described the way both halves of the feature need it.
 *
 * The physics only uses the face normals (to know which face is turned to the window) while the
 * interior shader intersects the same 20 planes exactly, so there is one source of truth for both.
 */
object DieGeometry {

    const val FACE_COUNT = 20

    /** Inradius over circumradius for a regular icosahedron. */
    const val INRADIUS_RATIO = 0.7946545f

    /** Distance from the centre to every face plane. */
    const val PLANE_DISTANCE = BallEngineTuning.DIE_CIRCUMRADIUS * INRADIUS_RATIO

    /** Edge length that follows from the circumradius. */
    private const val EDGE = BallEngineTuning.DIE_CIRCUMRADIUS / 0.9510565f

    /** Radius of the incircle of one triangular face — the room the answer text has. */
    const val FACE_INRADIUS = EDGE / 3.4641016f

    /**
     * Volume of the die, `(5/12)(3 + sqrt 5)` edges cubed. The liquid subtracts it: the die displaces
     * a real share of a nearly-full cavity, and counting that volume twice would over-fill the field.
     */
    const val VOLUME = 2.1816950f * EDGE * EDGE * EDGE

    private val SIGNS = floatArrayOf(1f, -1f)

    /**
     * Face normals: the 20 vertex directions of a dodecahedron, which is exactly the set of face
     * normals of its dual icosahedron.
     */
    val faceNormals: List<Vec3> = buildList(FACE_COUNT) {
        val phi = (1f + sqrt(5f)) / 2f
        val inversePhi = 1f / phi

        for (sx in SIGNS) {
            for (sy in SIGNS) {
                for (sz in SIGNS) {
                    add(Vec3(sx, sy, sz).normalized())
                }
            }
        }
        for (sa in SIGNS) {
            for (sb in SIGNS) {
                add(Vec3(0f, sa * inversePhi, sb * phi).normalized())
                add(Vec3(sa * inversePhi, sb * phi, 0f).normalized())
                add(Vec3(sa * phi, 0f, sb * inversePhi).normalized())
            }
        }
    }

    /** In-plane axes per face; the answer atlas maps its cell onto these. */
    val faceTangents: List<Vec3>
    val faceBitangents: List<Vec3>

    init {
        require(faceNormals.size == FACE_COUNT) {
            "An icosahedron has $FACE_COUNT faces, built ${faceNormals.size}"
        }
        val tangents = ArrayList<Vec3>(FACE_COUNT)
        val bitangents = ArrayList<Vec3>(FACE_COUNT)
        faceNormals.forEach { normal ->
            // Pick whichever reference is least parallel to the normal, so the basis stays stable.
            val reference = if (kotlin.math.abs(normal.y) < 0.9f) Vec3.UP else Vec3.FORWARD
            val tangent = (reference cross normal).normalized()
            tangents += tangent
            bitangents += (normal cross tangent).normalized()
        }
        faceTangents = tangents
        faceBitangents = bitangents
    }

    /** Centre of a face in the die's own frame. */
    fun faceCenter(index: Int): Vec3 = faceNormals[index] * PLANE_DISTANCE

    /** Flattens [vectors] into `3 * FACE_COUNT` floats for a uniform array upload. */
    fun flatten(vectors: List<Vec3>): FloatArray {
        val target = FloatArray(vectors.size * 3)
        vectors.forEachIndexed { index, vector -> vector.writeTo(target, index * 3) }
        return target
    }
}
