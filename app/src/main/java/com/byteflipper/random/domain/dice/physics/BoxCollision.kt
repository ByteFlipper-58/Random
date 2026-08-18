package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.MAX_CONTACTS_PER_PAIR
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Finds where the dice touch the tray and each other.
 *
 * Die against tray is eight corners against six planes, which needs no cleverness and happens to give
 * a face lying flat exactly the four contacts that hold it flat.
 *
 * Die against die is the separating-axis test over all fifteen candidate axes — three face normals
 * each and the nine cross products of their edges — followed by clipping one face against the other to
 * turn the winning axis into a patch of contact points rather than a single one. A single point is
 * what makes cubes in a pile rock and skate; four points is what makes them stack.
 *
 * Kept as a class rather than an object because of the scratch buffers: one instance per engine, used
 * from the one thread that steps it.
 */
class BoxCollision {

    private val referencePolygon = Array(MAX_POLYGON) { Vec3.ZERO }
    private val clippedPolygon = Array(MAX_POLYGON) { Vec3.ZERO }
    private val depths = FloatArray(MAX_POLYGON)
    private val axesA = Array(3) { Vec3.UP }
    private val axesB = Array(3) { Vec3.UP }

    /**
     * Contacts between die [index] and the tray around it.
     *
     * The floor and the walls carry their own pair. A die that hits the bottom lands on felt, which
     * gives a little and grips; one that hits a side hits the side of a box, which does neither — the
     * difference between the two is most of why a thrown die rattles instead of just dropping.
     */
    fun tray(
        index: Int,
        body: DiceBody,
        trayNearHalfX: Float,
        trayFarHalfX: Float,
        trayHalfZ: Float,
        ceiling: Float,
        floorRestitution: Float,
        floorFriction: Float,
        wallRestitution: Float,
        wallFriction: Float,
        out: ContactBuffer
    ) {
        val reach = body.halfExtent * SQRT3
        val sideSlope = (trayFarHalfX - trayNearHalfX) / (2f * trayHalfZ)
        val sideLength = sqrt(1f + sideSlope * sideSlope)
        val sideOffset = -((trayFarHalfX + trayNearHalfX) * 0.5f) / sideLength
        for (wall in 0 until WALL_COUNT) {
            val normal = when (wall) {
                1 -> Vec3(1f, 0f, -sideSlope) / sideLength
                2 -> Vec3(-1f, 0f, -sideSlope) / sideLength
                else -> WALL_NORMALS[wall]
            }
            val offset = when (wall) {
                0 -> 0f
                1, 2 -> sideOffset
                5 -> -ceiling
                else -> -trayHalfZ
            }
            // The whole die is clear of this wall: no corner of it can be through.
            if ((body.position dot normal) - offset > reach) continue

            val restitution = if (wall == FLOOR_WALL) floorRestitution else wallRestitution
            val friction = if (wall == FLOOR_WALL) floorFriction else wallFriction
            for (corner in 0 until CORNER_COUNT) {
                val point = body.corner(corner)
                val penetration = offset - (point dot normal)
                if (penetration <= 0f) continue
                emit(out, index, -1, point, normal, penetration, restitution, friction)
            }
        }
    }

    /**
     * Contacts between two dice, or nothing at all when they are apart.
     *
     * The fifteen axes are tested in order of how much they are worth trusting: a face axis that wins
     * by a hair over an edge axis keeps the win, because a face contact carries four points and an
     * edge contact carries one, and swapping between them frame to frame is what a pile of dice
     * shivering looks like.
     */
    fun dice(
        indexA: Int,
        a: DiceBody,
        indexB: Int,
        b: DiceBody,
        restitution: Float,
        friction: Float,
        out: ContactBuffer
    ) {
        val toB = b.position - a.position
        val reach = (a.halfExtent + b.halfExtent) * SQRT3
        if (toB.lengthSquared > reach * reach) return

        for (i in 0..2) {
            axesA[i] = a.axis(i)
            axesB[i] = b.axis(i)
        }

        var bestPenetration = Float.MAX_VALUE
        var bestKind = KIND_FACE_A
        var bestAxis = 0
        var bestEdge = 0
        // Whichever axis wins, this is it oriented from A towards B.
        var bestNormal = Vec3.UP

        for (i in 0..2) {
            val penetration = -separation(toB, axesA[i], a.halfExtent, b.halfExtent)
            if (penetration < 0f) return
            if (penetration >= bestPenetration) continue
            bestPenetration = penetration
            bestKind = KIND_FACE_A
            bestAxis = i
            bestNormal = orient(axesA[i], toB)
        }

        for (i in 0..2) {
            val penetration = -separation(toB, axesB[i], a.halfExtent, b.halfExtent)
            if (penetration < 0f) return
            if (penetration >= bestPenetration - AXIS_TIE) continue
            bestPenetration = penetration
            bestKind = KIND_FACE_B
            bestAxis = i
            bestNormal = orient(axesB[i], toB)
        }

        for (i in 0..2) {
            for (j in 0..2) {
                val cross = axesA[i] cross axesB[j]
                // Parallel edges: the axis is degenerate, and the face tests already covered it.
                if (cross.lengthSquared < PARALLEL_EPSILON) continue
                val axis = cross.normalized()
                val penetration = -separation(toB, axis, a.halfExtent, b.halfExtent)
                if (penetration < 0f) return
                if (penetration * EDGE_BIAS >= bestPenetration - AXIS_TIE) continue
                bestPenetration = penetration
                bestKind = KIND_EDGE
                bestAxis = i
                bestEdge = j
                bestNormal = orient(axis, toB)
            }
        }

        if (bestKind == KIND_EDGE) {
            edgeContact(
                indexA, a, indexB, b, bestAxis, bestEdge,
                bestNormal, bestPenetration, restitution, friction, out
            )
        } else {
            faceContacts(
                indexA, a, indexB, b, bestKind == KIND_FACE_A, bestAxis,
                bestNormal, bestPenetration, restitution, friction, out
            )
        }
    }

    /**
     * The one contact an edge-on-edge overlap deserves, where the two edges pass closest.
     *
     * The edges are found from the winning axis: the die's extreme corner along it, with the component
     * along the edge left free, is one end of the edge in question.
     */
    private fun edgeContact(
        indexA: Int,
        a: DiceBody,
        indexB: Int,
        b: DiceBody,
        axisA: Int,
        axisB: Int,
        normal: Vec3,
        penetration: Float,
        restitution: Float,
        friction: Float,
        out: ContactBuffer
    ) {
        var centerA = a.position
        for (k in 0..2) {
            if (k == axisA) continue
            centerA += axesA[k] * (a.halfExtent * signOf(normal dot axesA[k]))
        }
        var centerB = b.position
        for (k in 0..2) {
            if (k == axisB) continue
            centerB -= axesB[k] * (b.halfExtent * signOf(normal dot axesB[k]))
        }

        val point = closestPoint(
            centerA, axesA[axisA], a.halfExtent,
            centerB, axesB[axisB], b.halfExtent
        )
        emit(out, indexA, indexB, point, -normal, penetration, restitution, friction)
    }

    /** Where two segments come closest, as the midpoint between the two nearest points. */
    private fun closestPoint(
        centerA: Vec3,
        directionA: Vec3,
        halfA: Float,
        centerB: Vec3,
        directionB: Vec3,
        halfB: Float
    ): Vec3 {
        val delta = centerA - centerB
        val alignment = directionA dot directionB
        val alongA = directionA dot delta
        val alongB = directionB dot delta
        val denominator = 1f - alignment * alignment

        // Parallel edges have no single nearest pair, so take the midpoint of the overlap instead.
        var t = if (denominator > PARALLEL_EPSILON) {
            (alongB - alongA * alignment) / denominator
        } else {
            0f
        }
        t = t.coerceIn(-halfB, halfB)
        val s = (t * alignment - alongA).coerceIn(-halfA, halfA)
        t = (alongB + s * alignment).coerceIn(-halfB, halfB)

        return ((centerA + directionA * s) + (centerB + directionB * t)) * 0.5f
    }

    /**
     * The patch of contact a face-on overlap deserves.
     *
     * The winning axis names a face on one die; the face of the other die most nearly turned towards it
     * is clipped down to the part that lies over it, and whatever survives with something still through
     * the plane becomes the manifold. Flat on flat gives four points, a corner into a face gives one,
     * and everything between falls out of the same pass.
     */
    private fun faceContacts(
        indexA: Int,
        a: DiceBody,
        indexB: Int,
        b: DiceBody,
        referenceIsA: Boolean,
        axis: Int,
        normal: Vec3,
        penetration: Float,
        restitution: Float,
        friction: Float,
        out: ContactBuffer
    ) {
        val reference = if (referenceIsA) a else b
        val referenceAxes = if (referenceIsA) axesA else axesB
        val incident = if (referenceIsA) b else a
        val incidentAxes = if (referenceIsA) axesB else axesA

        // Out of the reference die, towards the other one.
        val faceNormal = if (referenceIsA) normal else -normal
        val referenceHalf = reference.halfExtent
        val incidentHalf = incident.halfExtent
        val faceCenter = reference.position + faceNormal * referenceHalf
        val u = referenceAxes[(axis + 1) % 3]
        val v = referenceAxes[(axis + 2) % 3]

        var incidentAxis = 0
        var incidentNormal = incidentAxes[0]
        var mostOpposed = Float.MAX_VALUE
        for (i in 0..2) {
            val alignment = incidentAxes[i] dot faceNormal
            if (alignment < mostOpposed) {
                mostOpposed = alignment
                incidentAxis = i
                incidentNormal = incidentAxes[i]
            }
            if (-alignment < mostOpposed) {
                mostOpposed = -alignment
                incidentAxis = i
                incidentNormal = -incidentAxes[i]
            }
        }

        val incidentCenter = incident.position + incidentNormal * incidentHalf
        val incidentU = incidentAxes[(incidentAxis + 1) % 3] * incidentHalf
        val incidentV = incidentAxes[(incidentAxis + 2) % 3] * incidentHalf
        referencePolygon[0] = incidentCenter + incidentU + incidentV
        referencePolygon[1] = incidentCenter - incidentU + incidentV
        referencePolygon[2] = incidentCenter - incidentU - incidentV
        referencePolygon[3] = incidentCenter + incidentU - incidentV

        var count = clip(referencePolygon, 4, faceCenter, u, referenceHalf, clippedPolygon)
        count = clip(clippedPolygon, count, faceCenter, -u, referenceHalf, referencePolygon)
        count = clip(referencePolygon, count, faceCenter, v, referenceHalf, clippedPolygon)
        count = clip(clippedPolygon, count, faceCenter, -v, referenceHalf, referencePolygon)

        var kept = 0
        for (i in 0 until count) {
            val point = referencePolygon[i]
            val depth = referenceHalf - ((point - reference.position) dot faceNormal)
            if (depth <= 0f) continue
            referencePolygon[kept] = point
            depths[kept] = depth
            kept++
        }

        if (kept == 0) {
            // The axis test says they overlap, so hand the solver the one point it can be sure of.
            emit(
                out, indexA, indexB, support(a, axesA, normal),
                -normal, penetration, restitution, friction
            )
            return
        }

        // More than four is a clipping artefact of a near-parallel pair; the deepest are the real ones.
        val limit = min(kept, MAX_CONTACTS_PER_PAIR)
        for (slot in 0 until limit) {
            var deepest = slot
            for (i in slot + 1 until kept) {
                if (depths[i] > depths[deepest]) deepest = i
            }
            if (deepest != slot) {
                val point = referencePolygon[slot]
                referencePolygon[slot] = referencePolygon[deepest]
                referencePolygon[deepest] = point
                val depth = depths[slot]
                depths[slot] = depths[deepest]
                depths[deepest] = depth
            }
            emit(
                out, indexA, indexB, referencePolygon[slot],
                -normal, depths[slot], restitution, friction
            )
        }
    }

    /**
     * Sutherland–Hodgman against one half-plane, keeping what satisfies
     * `dot(point - origin, axis) <= limit`.
     */
    private fun clip(
        source: Array<Vec3>,
        count: Int,
        origin: Vec3,
        axis: Vec3,
        limit: Float,
        target: Array<Vec3>
    ): Int {
        var written = 0
        for (i in 0 until count) {
            if (written >= target.size - 1) break
            val current = source[i]
            val next = source[(i + 1) % count]
            val here = ((current - origin) dot axis) - limit
            val there = ((next - origin) dot axis) - limit
            if (here <= 0f) target[written++] = current
            // The edge crosses the plane, so the crossing itself is a vertex of what is left.
            if (here * there < 0f) {
                target[written++] = current + (next - current) * (here / (here - there))
            }
        }
        return written
    }

    /** The die's furthest corner along [direction]. */
    private fun support(body: DiceBody, axes: Array<Vec3>, direction: Vec3): Vec3 {
        var point = body.position
        for (k in 0..2) {
            point += axes[k] * (body.halfExtent * signOf(direction dot axes[k]))
        }
        return point
    }

    /** How far apart the two dice are along [axis]; negative means they overlap by that much. */
    private fun separation(toB: Vec3, axis: Vec3, halfA: Float, halfB: Float): Float =
        abs(toB dot axis) - extent(axesA, halfA, axis) - extent(axesB, halfB, axis)

    /** Half the die's shadow on [axis] — the same sum for a cube whichever way it is turned. */
    private fun extent(axes: Array<Vec3>, half: Float, axis: Vec3): Float =
        half * (abs(axis dot axes[0]) + abs(axis dot axes[1]) + abs(axis dot axes[2]))

    private fun orient(axis: Vec3, toB: Vec3): Vec3 = if ((toB dot axis) >= 0f) axis else -axis

    private fun signOf(value: Float): Float = if (value >= 0f) 1f else -1f

    private fun emit(
        out: ContactBuffer,
        a: Int,
        b: Int,
        point: Vec3,
        normal: Vec3,
        penetration: Float,
        restitution: Float,
        friction: Float
    ) {
        val contact = out.next()
        contact.a = a
        contact.b = b
        contact.point = point
        contact.normal = normal
        contact.penetration = penetration
        contact.restitution = restitution
        contact.friction = friction
        contact.normalImpulse = 0f
    }

    private companion object {
        const val CORNER_COUNT = 8
        const val WALL_COUNT = 6
        const val MAX_POLYGON = 8
        val SQRT3 = sqrt(3f)

        /** Index of the floor in [WALL_NORMALS]; the other five are walls. */
        const val FLOOR_WALL = 0

        const val KIND_FACE_A = 0
        const val KIND_FACE_B = 1
        const val KIND_EDGE = 2

        /** Below this the two edges are parallel and their cross product says nothing. */
        const val PARALLEL_EPSILON = 1e-6f

        /** Inward normals: floor, the four sides, then the lid. */
        val WALL_NORMALS = listOf(
            Vec3(0f, 1f, 0f),
            Vec3(1f, 0f, 0f),
            Vec3(-1f, 0f, 0f),
            Vec3(0f, 0f, 1f),
            Vec3(0f, 0f, -1f),
            Vec3(0f, -1f, 0f)
        )

        /** How much shallower an edge-edge axis has to be before it beats a face axis. */
        const val EDGE_BIAS = 1.02f

        /** Slack that keeps a near tie between two axes from flipping every step. */
        const val AXIS_TIE = 1e-4f
    }
}
