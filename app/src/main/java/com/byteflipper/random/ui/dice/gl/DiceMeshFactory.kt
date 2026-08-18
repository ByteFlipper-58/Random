package com.byteflipper.random.ui.dice.gl

import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.dice.physics.DieFaces
import com.byteflipper.random.ui.gl.Mesh
import com.byteflipper.random.ui.gl.VertexAttribute
import kotlin.math.PI
import kotlin.math.tan

/** Geometry the dice tray needs. Every factory has to be called on the GL thread. */
object DiceMeshFactory {

    /**
     * How much of the half-extent the rounded corner takes.
     *
     * A cube with sharp corners reads as a box rather than as a die; the rounding is most of what says
     * "moulded plastic", and it is also what gives the silhouette a highlight to catch as the die turns.
     */
    const val CORNER_RADIUS = 0.19f

    /**
     * A die: the unit cube with its edges and corners rounded off.
     *
     * Built as six patches, each a grid over its face in `[-1, 1]²`, pushed onto the rounded solid by
     * the standard rounded-box map — clamp the point into the cube's flat core, then step [CORNER_RADIUS]
     * back out along the direction it was clamped from. On the flat middle of a face that direction is
     * the face normal and nothing moves; along an edge it sweeps a quarter cylinder; at a corner, an
     * octant of a sphere. So one expression covers all three, and the normal falls out of it for free.
     *
     * Neighbouring patches agree exactly along a shared edge — both faces map the same cube point — so
     * there is no seam to see and no crack for the background to show through.
     *
     * The grid is coarse across the flat interior, which is planar and needs only its corners, and fine
     * across the rounded band, sampled so that [arcSegments] steps are spread evenly over the quarter
     * turn rather than evenly across the face.
     *
     * Each vertex also carries the face's own 2D coordinates and the number printed on it, which is what
     * lets the fragment shader lay out the pips without a texture.
     */
    fun roundedDie(arcSegments: Int = 4): Mesh {
        require(arcSegments >= 1) { "A rounded corner needs at least one segment" }

        val core = 1f - CORNER_RADIUS
        val samples = coordinates(core, arcSegments)
        val stride = samples.size

        val vertices = FloatArray(DieFaces.FACE_COUNT * stride * stride * FLOATS_PER_VERTEX)
        val indices = ShortArray(DieFaces.FACE_COUNT * (stride - 1) * (stride - 1) * 6)
        var vertex = 0
        var index = 0

        for (face in 0 until DieFaces.FACE_COUNT) {
            val normal = DieFaces.normals[face]
            val u = FACE_TANGENTS[face]
            val v = FACE_BITANGENTS[face]
            val pips = DieFaces.values[face].toFloat()
            val base = face * stride * stride

            for (i in 0 until stride) {
                for (j in 0 until stride) {
                    val s = samples[i]
                    val t = samples[j]
                    val point = normal + u * s + v * t
                    val clamped = Vec3(
                        point.x.coerceIn(-core, core),
                        point.y.coerceIn(-core, core),
                        point.z.coerceIn(-core, core)
                    )
                    val outward = (point - clamped).normalized(normal)
                    val position = clamped + outward * CORNER_RADIUS

                    vertices[vertex++] = position.x
                    vertices[vertex++] = position.y
                    vertices[vertex++] = position.z
                    vertices[vertex++] = outward.x
                    vertices[vertex++] = outward.y
                    vertices[vertex++] = outward.z
                    vertices[vertex++] = s
                    vertices[vertex++] = t
                    vertices[vertex++] = pips
                }
            }

            // Counter-clockwise seen from outside, since tangent x bitangent is the face normal.
            for (i in 0 until stride - 1) {
                for (j in 0 until stride - 1) {
                    val corner = base + i * stride + j
                    indices[index++] = corner.toShort()
                    indices[index++] = (corner + stride).toShort()
                    indices[index++] = (corner + stride + 1).toShort()
                    indices[index++] = corner.toShort()
                    indices[index++] = (corner + stride + 1).toShort()
                    indices[index++] = (corner + 1).toShort()
                }
            }
        }

        return Mesh(
            vertices = vertices,
            indices = indices,
            attributes = listOf(
                VertexAttribute("aPosition", 3),
                VertexAttribute("aNormal", 3),
                VertexAttribute("aUv", 2),
                VertexAttribute("aPips", 1)
            )
        )
    }

    /**
     * The floor, as one quad on `y = 0` reaching [halfExtent] each way.
     *
     * Deliberately far larger than the tray it holds: the camera looks down steeply enough that the
     * horizon never comes into shot, so this one quad is the whole background, and the shader paints the
     * tray's felt onto it rather than the tray being geometry of its own.
     */
    fun floorQuad(halfExtent: Float): Mesh = Mesh(
        vertices = floatArrayOf(
            -halfExtent, 0f, -halfExtent,
            -halfExtent, 0f, halfExtent,
            halfExtent, 0f, halfExtent,
            halfExtent, 0f, -halfExtent
        ),
        indices = shortArrayOf(0, 1, 2, 0, 2, 3),
        attributes = listOf(VertexAttribute("aPosition", 3))
    )

    /** Screen-filling quad in clip space, for the backdrop the floor is drawn over. */
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
     * Where a face is sampled along one of its axes: its two flat edges, then the rounded band either
     * side of them.
     *
     * The band's samples are spaced by angle, not by distance — the offset that turns the normal by
     * `θ` is `radius * tan(θ)` — so the quarter turn is divided evenly and the facets come out the same
     * width all the way round the corner.
     */
    private fun coordinates(core: Float, arcSegments: Int): FloatArray {
        val quarter = (PI / 4.0).toFloat()
        val band = FloatArray(arcSegments) { step ->
            core + CORNER_RADIUS * tan(quarter * (step + 1) / arcSegments)
        }
        val samples = FloatArray(2 + 2 * arcSegments)
        for (step in 0 until arcSegments) {
            samples[step] = -band[arcSegments - 1 - step]
        }
        samples[arcSegments] = -core
        samples[arcSegments + 1] = core
        for (step in 0 until arcSegments) {
            samples[arcSegments + 2 + step] = band[step]
        }
        return samples
    }

    /** Position, normal, face coordinates, printed number. */
    private const val FLOATS_PER_VERTEX = 9

    /**
     * In-plane axes of each face, in [DieFaces.normals] order and paired so that tangent cross
     * bitangent is the face's own normal — which is what makes one winding rule work for all six.
     */
    private val FACE_TANGENTS = listOf(
        Vec3(0f, 1f, 0f),
        Vec3(0f, 0f, 1f),
        Vec3(0f, 0f, 1f),
        Vec3(1f, 0f, 0f),
        Vec3(1f, 0f, 0f),
        Vec3(0f, 1f, 0f)
    )

    private val FACE_BITANGENTS = listOf(
        Vec3(0f, 0f, 1f),
        Vec3(0f, 1f, 0f),
        Vec3(1f, 0f, 0f),
        Vec3(0f, 0f, 1f),
        Vec3(0f, 1f, 0f),
        Vec3(1f, 0f, 0f)
    )
}
