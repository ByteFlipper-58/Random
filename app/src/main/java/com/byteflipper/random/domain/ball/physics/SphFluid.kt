package com.byteflipper.random.domain.ball.physics

import com.byteflipper.random.domain.physics.Vec3
import java.util.Arrays
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Weakly-compressible SPH for the liquid inside the ball.
 *
 * The bulk level is drawn from [BallCavity]'s analytic plane, so these particles only have to sell
 * the sloshing on top of it. That is what lets the pressure term stay this soft: the liquid is meant
 * to look like syrup, and a soft, heavily damped fluid is both the right look and stable at the
 * engine's own step. No amount of shaking can make the level drift, because the level is not stored
 * here.
 *
 * Particles live in flat arrays rather than objects: a few hundred of them stepped 120 times a
 * second would otherwise allocate megabytes a minute for nothing.
 *
 * Not thread-safe, and not meant to be — the engine owns it and steps it on one thread.
 */
class SphFluid(
    private val cavityRadius: Float = BallEngineTuning.CAVITY_RADIUS,
    private val capacity: Int = BallEngineTuning.MAX_PARTICLES,
    private val random: Random = Random.Default
) {

    private val px = FloatArray(capacity)
    private val py = FloatArray(capacity)
    private val pz = FloatArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)
    private val vz = FloatArray(capacity)

    /** Accelerations for the current step, filled by the force pass. */
    private val ax = FloatArray(capacity)
    private val ay = FloatArray(capacity)
    private val az = FloatArray(capacity)

    /** XSPH velocity corrections, which stand in for viscosity. */
    private val sx = FloatArray(capacity)
    private val sy = FloatArray(capacity)
    private val sz = FloatArray(capacity)

    private val density = FloatArray(capacity)
    private val pressure = FloatArray(capacity)

    /** Live particles. The arrays are always sized for the largest tier, so retuning never allocates. */
    var count: Int = 0
        private set

    /** Kernel support radius, derived from [count] so the packing looks the same on every tier. */
    var supportRadius: Float = 0.3f
        private set

    /** Rest distance between particles. */
    var spacing: Float = 0.2f
        private set

    /**
     * Volume of liquid in the cavity, with the die's own bulk taken out. The cavity is nearly full,
     * so the die displaces a real share of it, and a particle carrying volume the die is already
     * occupying would push the whole field above one and drown the air pocket.
     */
    private val fluidVolume = max(
        BallEngineTuning.FLUID_FILL * 4f / 3f * PI.toFloat() *
            cavityRadius * cavityRadius * cavityRadius - DieGeometry.VOLUME,
        0.05f
    )

    private var particleVolume = 0f
    private var particleMass = 0f

    /**
     * Density a well-packed interior settles at. Exactly 1 by construction: each particle carries the
     * volume it occupies as its mass, which makes the whole field a volume fraction and saves the
     * shader a conversion.
     */
    private val restDensity = 1f

    private var gridDim = 1
    private var cellSize = 1f

    private val cellStart = IntArray(MAX_CELLS + 1)
    private val cellCursor = IntArray(MAX_CELLS)
    private val sorted = IntArray(capacity)
    private val cellOf = IntArray(capacity)

    /**
     * Refills the cavity with [particleCount] particles, leaving the space [dieCenter] occupies free.
     *
     * Called when the quality tier changes or the ball is reset, never per frame.
     */
    fun seed(
        particleCount: Int,
        cavity: BallCavity,
        up: Vec3,
        dieCenter: Vec3 = Vec3.ZERO
    ) {
        count = particleCount.coerceIn(0, capacity)
        if (count == 0) return

        particleVolume = fluidVolume / count
        particleMass = particleVolume
        spacing = particleVolume.pow(1f / 3f)
        supportRadius = BallEngineTuning.SPH_SUPPORT_SCALE * spacing

        // Cells are never smaller than the kernel, so a 3x3x3 walk always covers every neighbour.
        gridDim = ceil(2f * cavityRadius / supportRadius).toInt().coerceIn(1, MAX_GRID_DIM)
        cellSize = max(supportRadius, 2f * cavityRadius / gridDim)

        placeParticles(cavity, up, dieCenter)
    }

    /**
     * One fixed step: pressure and viscosity between particles, the effective gravity they feel, the
     * spinning glass dragging the liquid around, then the collisions with the wall and with the die.
     *
     * [acceleration] is a full vector rather than a direction: it carries gravity *and* whatever the
     * phone itself is doing, which is what makes a shake actually throw the liquid about.
     */
    fun step(
        deltaSeconds: Float,
        acceleration: Vec3,
        die: RigidBody,
        shellAngularVelocity: Vec3
    ) {
        if (count == 0 || deltaSeconds <= 0f) return

        buildGrid()
        computeDensity()
        accumulateForces(acceleration, shellAngularVelocity)
        integrate(deltaSeconds)
        resolveDie(die)
        resolveWall()
    }

    /**
     * Splats the particles into a cubic density grid covering the whole cavity.
     *
     * Each particle deposits exactly the volume it stands for, so [target] comes out as a volume
     * fraction: 1 deep inside the liquid, 0 in the air, and the half-way level is the surface. That
     * is what lets the shader use one iso level for every quality tier.
     */
    fun splat(target: FloatArray, resolution: Int) {
        Arrays.fill(target, 0f)
        if (count == 0 || resolution <= 0) return

        val voxel = 2f * cavityRadius / resolution
        val radius = BallEngineTuning.SPLAT_RADIUS_SCALE * spacing
        val amplitude = particleVolume / (KERNEL_INTEGRAL * radius * radius * radius)
        val radiusSquared = radius * radius
        val inverseRadiusSquared = 1f / radiusSquared
        val last = resolution - 1

        for (index in 0 until count) {
            val x = px[index]
            val y = py[index]
            val z = pz[index]

            val minX = voxelIndex(x - radius, voxel, last)
            val maxX = voxelIndex(x + radius, voxel, last)
            val minY = voxelIndex(y - radius, voxel, last)
            val maxY = voxelIndex(y + radius, voxel, last)
            val minZ = voxelIndex(z - radius, voxel, last)
            val maxZ = voxelIndex(z + radius, voxel, last)

            for (kz in minZ..maxZ) {
                val dz = voxelCenter(kz, voxel) - z
                val dz2 = dz * dz
                if (dz2 > radiusSquared) continue
                for (ky in minY..maxY) {
                    val dy = voxelCenter(ky, voxel) - y
                    val remaining = radiusSquared - dz2 - dy * dy
                    if (remaining <= 0f) continue
                    val rowBase = (kz * resolution + ky) * resolution
                    for (kx in minX..maxX) {
                        val dx = voxelCenter(kx, voxel) - x
                        val dx2 = dx * dx
                        if (dx2 > remaining) continue
                        val falloff = 1f - (dz2 + dy * dy + dx2) * inverseRadiusSquared
                        target[rowBase + kx] += amplitude * falloff * falloff
                    }
                }
            }
        }
    }

    fun positionAt(index: Int): Vec3 = Vec3(px[index], py[index], pz[index])

    fun velocityAt(index: Int): Vec3 = Vec3(vx[index], vy[index], vz[index])

    fun densityAt(index: Int): Float = density[index]

    /** Mean density over every particle; the surface layer drags it below [restDensity]. */
    fun averageDensity(): Float {
        if (count == 0) return 0f
        var sum = 0f
        for (index in 0 until count) sum += density[index]
        return sum / count
    }

    private fun placeParticles(cavity: BallCavity, up: Vec3, dieCenter: Vec3) {
        // A lattice a shade tighter than the rest spacing, so there are always enough sites to pick
        // from and the fill can be spread over the whole liquid instead of stacking from the bottom.
        val step = spacing * 0.94f
        val limit = cavityRadius - BallEngineTuning.PARTICLE_RADIUS
        val perAxis = ceil(2f * limit / step).toInt()
        val start = -0.5f * (perAxis - 1) * step
        // The die is solid, so a site inside it is not a place liquid can be.
        val solid = DieGeometry.PLANE_DISTANCE + BallEngineTuning.PARTICLE_RADIUS
        val solidSquared = solid * solid

        val sites = ArrayList<Vec3>(count * 2)
        for (iz in 0 until perAxis) {
            for (iy in 0 until perAxis) {
                for (ix in 0 until perAxis) {
                    val candidate = Vec3(start + ix * step, start + iy * step, start + iz * step)
                    if (candidate.lengthSquared > limit * limit) continue
                    if (cavity.depthBelowSurface(candidate, up) <= 0f) continue
                    if ((candidate - dieCenter).lengthSquared < solidSquared) continue
                    sites += candidate
                }
            }
        }

        val jitter = step * 0.16f
        for (index in 0 until count) {
            // Even stride through the sites, so a short list still fills the liquid uniformly.
            val site = if (sites.isEmpty()) {
                Vec3.ZERO
            } else {
                sites[(index.toLong() * sites.size / count).toInt().coerceAtMost(sites.size - 1)]
            }
            px[index] = site.x + (random.nextFloat() * 2f - 1f) * jitter
            py[index] = site.y + (random.nextFloat() * 2f - 1f) * jitter
            pz[index] = site.z + (random.nextFloat() * 2f - 1f) * jitter
            vx[index] = 0f
            vy[index] = 0f
            vz[index] = 0f
            density[index] = restDensity
            pressure[index] = 0f
        }
        resolveWall()
    }

    private fun buildGrid() {
        val cells = gridDim * gridDim * gridDim
        Arrays.fill(cellStart, 0, cells + 1, 0)

        for (index in 0 until count) {
            val cell = cellIndexOf(px[index], py[index], pz[index])
            cellOf[index] = cell
            cellStart[cell + 1]++
        }
        for (cell in 0 until cells) {
            cellStart[cell + 1] += cellStart[cell]
            cellCursor[cell] = cellStart[cell]
        }
        for (index in 0 until count) {
            sorted[cellCursor[cellOf[index]]++] = index
        }
    }

    private fun computeDensity() {
        val h = supportRadius
        val h2 = h * h
        val poly6 = 315f / (64f * PI.toFloat() * h.pow(9))

        for (index in 0 until count) {
            val x = px[index]
            val y = py[index]
            val z = pz[index]
            // The particle's own contribution, which is the kernel at distance zero.
            var sum = h2 * h2 * h2

            forEachNeighbour(index) { other ->
                val dx = x - px[other]
                val dy = y - py[other]
                val dz = z - pz[other]
                val distanceSquared = dx * dx + dy * dy + dz * dz
                if (distanceSquared < h2) {
                    val gap = h2 - distanceSquared
                    sum += gap * gap * gap
                }
            }

            // Never below a fraction of rest: a lone particle would otherwise divide by almost zero.
            density[index] = max(particleMass * poly6 * sum, restDensity * 0.12f)
        }
    }

    private fun accumulateForces(acceleration: Vec3, shellAngularVelocity: Vec3) {
        val h = supportRadius
        val h2 = h * h
        val poly6 = 315f / (64f * PI.toFloat() * h.pow(9))
        val spiky = -45f / (PI.toFloat() * h.pow(6))

        for (index in 0 until count) {
            pressure[index] =
                max(0f, BallEngineTuning.SPH_STIFFNESS * (density[index] - restDensity))
        }

        val gravityX = acceleration.x
        val gravityY = acceleration.y
        val gravityZ = acceleration.z
        val spinX = shellAngularVelocity.x
        val spinY = shellAngularVelocity.y
        val spinZ = shellAngularVelocity.z
        val wallLimit = cavityRadius - BallEngineTuning.PARTICLE_RADIUS

        for (index in 0 until count) {
            val x = px[index]
            val y = py[index]
            val z = pz[index]
            val ownDensity = density[index]
            val ownTerm = pressure[index] / (ownDensity * ownDensity)

            var accelX = gravityX
            var accelY = gravityY
            var accelZ = gravityZ
            var smoothX = 0f
            var smoothY = 0f
            var smoothZ = 0f

            forEachNeighbour(index) { other ->
                val dx = x - px[other]
                val dy = y - py[other]
                val dz = z - pz[other]
                val distanceSquared = dx * dx + dy * dy + dz * dz
                if (distanceSquared < h2 && distanceSquared > 1e-12f) {
                    val distance = sqrt(distanceSquared)
                    val otherDensity = density[other]
                    val shared = ownTerm + pressure[other] / (otherDensity * otherDensity)
                    val gap = h - distance
                    // Spiky gradient points from this particle towards its neighbour, so the minus
                    // sign in front turns positive pressure into a push apart.
                    val gradient = spiky * gap * gap / distance
                    val scale = -particleMass * shared * gradient
                    accelX += dx * scale
                    accelY += dy * scale
                    accelZ += dz * scale

                    val kernelGap = h2 - distanceSquared
                    val weight = particleMass / otherDensity * poly6 *
                        kernelGap * kernelGap * kernelGap
                    smoothX += (vx[other] - vx[index]) * weight
                    smoothY += (vy[other] - vy[index]) * weight
                    smoothZ += (vz[other] - vz[index]) * weight
                }
            }

            // The glass drags the liquid next to it along as the shell turns.
            val wallDistance = sqrt(x * x + y * y + z * z)
            val proximity = (wallDistance / wallLimit).coerceIn(0f, 1f).let { it * it * it * it }
            if (proximity > 1e-3f) {
                val couple = BallEngineTuning.SPH_SHELL_COUPLING * proximity
                accelX += (spinY * z - spinZ * y - vx[index]) * couple
                accelY += (spinZ * x - spinX * z - vy[index]) * couple
                accelZ += (spinX * y - spinY * x - vz[index]) * couple
            }

            // A single frame of a bad pressure spike must not be able to launch a particle.
            val magnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
            if (magnitude > MAX_ACCELERATION) {
                val trim = MAX_ACCELERATION / magnitude
                accelX *= trim
                accelY *= trim
                accelZ *= trim
            }

            ax[index] = accelX
            ay[index] = accelY
            az[index] = accelZ
            sx[index] = smoothX
            sy[index] = smoothY
            sz[index] = smoothZ
        }
    }

    private fun integrate(deltaSeconds: Float) {
        val damping = exp(-BallEngineTuning.SPH_LINEAR_DRAG * deltaSeconds)
        val viscosity = BallEngineTuning.SPH_VISCOSITY
        val maxSpeed = BallEngineTuning.SPH_MAX_SPEED

        for (index in 0 until count) {
            var velocityX = (vx[index] + ax[index] * deltaSeconds + sx[index] * viscosity) * damping
            var velocityY = (vy[index] + ay[index] * deltaSeconds + sy[index] * viscosity) * damping
            var velocityZ = (vz[index] + az[index] * deltaSeconds + sz[index] * viscosity) * damping

            val speed = sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ)
            if (speed > maxSpeed) {
                val trim = maxSpeed / speed
                velocityX *= trim
                velocityY *= trim
                velocityZ *= trim
            }

            vx[index] = velocityX
            vy[index] = velocityY
            vz[index] = velocityZ
            px[index] += velocityX * deltaSeconds
            py[index] += velocityY * deltaSeconds
            pz[index] += velocityZ * deltaSeconds

            if (!px[index].isFinite() || !py[index].isFinite() || !pz[index].isFinite()) {
                // Whatever went wrong, one particle back at the centre is invisible; a NaN is not.
                px[index] = 0f
                py[index] = 0f
                pz[index] = 0f
                vx[index] = 0f
                vy[index] = 0f
                vz[index] = 0f
            }
        }
    }

    /**
     * Keeps the particles out of the die and hands the die back what it took from them, which is
     * what makes a splash rock the answer.
     */
    private fun resolveDie(die: RigidBody) {
        val solidRadius = DieGeometry.PLANE_DISTANCE + BallEngineTuning.PARTICLE_RADIUS
        val solidSquared = solidRadius * solidRadius
        val centerX = die.position.x
        val centerY = die.position.y
        val centerZ = die.position.z
        val spinX = die.angularVelocity.x
        val spinY = die.angularVelocity.y
        val spinZ = die.angularVelocity.z

        var impulseX = 0f
        var impulseY = 0f
        var impulseZ = 0f
        var torqueX = 0f
        var torqueY = 0f
        var torqueZ = 0f

        for (index in 0 until count) {
            val offsetX = px[index] - centerX
            val offsetY = py[index] - centerY
            val offsetZ = pz[index] - centerZ
            val distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ
            if (distanceSquared >= solidSquared || distanceSquared < 1e-12f) continue

            val distance = sqrt(distanceSquared)
            val normalX = offsetX / distance
            val normalY = offsetY / distance
            val normalZ = offsetZ / distance

            px[index] = centerX + normalX * solidRadius
            py[index] = centerY + normalY * solidRadius
            pz[index] = centerZ + normalZ * solidRadius

            // Velocity of the die at the contact, spin included.
            val contactX = die.velocity.x + (spinY * offsetZ - spinZ * offsetY)
            val contactY = die.velocity.y + (spinZ * offsetX - spinX * offsetZ)
            val contactZ = die.velocity.z + (spinX * offsetY - spinY * offsetX)

            val relativeNormal = (vx[index] - contactX) * normalX +
                (vy[index] - contactY) * normalY +
                (vz[index] - contactZ) * normalZ
            if (relativeNormal >= 0f) continue

            val change = -(1f + BallEngineTuning.SPH_WALL_RESTITUTION) * relativeNormal
            vx[index] += normalX * change
            vy[index] += normalY * change
            vz[index] += normalZ * change

            val magnitude = particleMass * change
            impulseX -= normalX * magnitude
            impulseY -= normalY * magnitude
            impulseZ -= normalZ * magnitude
            torqueX -= (offsetY * normalZ - offsetZ * normalY) * magnitude
            torqueY -= (offsetZ * normalX - offsetX * normalZ) * magnitude
            torqueZ -= (offsetX * normalY - offsetY * normalX) * magnitude
        }

        val coupling = BallEngineTuning.SPH_DIE_COUPLING
        if (impulseX != 0f || impulseY != 0f || impulseZ != 0f) {
            die.applyImpulse(Vec3(impulseX, impulseY, impulseZ) * coupling)
            die.applyAngularImpulse(Vec3(torqueX, torqueY, torqueZ) * coupling)
        }
    }

    private fun resolveWall() {
        val limit = cavityRadius - BallEngineTuning.PARTICLE_RADIUS
        val limitSquared = limit * limit
        val restitution = BallEngineTuning.SPH_WALL_RESTITUTION
        val friction = BallEngineTuning.SPH_WALL_FRICTION

        for (index in 0 until count) {
            val x = px[index]
            val y = py[index]
            val z = pz[index]
            val distanceSquared = x * x + y * y + z * z
            if (distanceSquared <= limitSquared) continue

            val distance = sqrt(distanceSquared)
            if (distance < Vec3.EPSILON) continue
            val normalX = x / distance
            val normalY = y / distance
            val normalZ = z / distance
            px[index] = normalX * limit
            py[index] = normalY * limit
            pz[index] = normalZ * limit

            val normalSpeed = vx[index] * normalX + vy[index] * normalY + vz[index] * normalZ
            if (normalSpeed <= 0f) continue
            val slide = 1f - friction
            vx[index] = (vx[index] - normalX * normalSpeed) * slide - normalX * normalSpeed * restitution
            vy[index] = (vy[index] - normalY * normalSpeed) * slide - normalY * normalSpeed * restitution
            vz[index] = (vz[index] - normalZ * normalSpeed) * slide - normalZ * normalSpeed * restitution
        }
    }

    /** Visits every particle in the 27 cells around [index]'s own, itself excluded. */
    private inline fun forEachNeighbour(index: Int, action: (Int) -> Unit) {
        val cell = cellOf[index]
        val planeStride = gridDim * gridDim
        val cellZ = cell / planeStride
        val cellY = (cell - cellZ * planeStride) / gridDim
        val cellX = cell % gridDim

        for (offsetZ in -1..1) {
            val z = cellZ + offsetZ
            if (z < 0 || z >= gridDim) continue
            for (offsetY in -1..1) {
                val y = cellY + offsetY
                if (y < 0 || y >= gridDim) continue
                val rowBase = z * planeStride + y * gridDim
                for (offsetX in -1..1) {
                    val x = cellX + offsetX
                    if (x < 0 || x >= gridDim) continue
                    var slot = cellStart[rowBase + x]
                    val end = cellStart[rowBase + x + 1]
                    while (slot < end) {
                        val other = sorted[slot]
                        if (other != index) action(other)
                        slot++
                    }
                }
            }
        }
    }

    private fun cellIndexOf(x: Float, y: Float, z: Float): Int {
        val cellX = axisCell(x)
        val cellY = axisCell(y)
        val cellZ = axisCell(z)
        return (cellZ * gridDim + cellY) * gridDim + cellX
    }

    private fun axisCell(value: Float): Int =
        ((value + cavityRadius) / cellSize).toInt().coerceIn(0, gridDim - 1)

    private fun voxelCenter(index: Int, voxel: Float): Float =
        -cavityRadius + (index + 0.5f) * voxel

    private fun voxelIndex(coordinate: Float, voxel: Float, last: Int): Int =
        floor((coordinate + cavityRadius) / voxel).toInt().coerceIn(0, last)

    private companion object {
        /** Enough cells for the densest tier; the grid never grows past this. */
        const val MAX_GRID_DIM = 8
        const val MAX_CELLS = MAX_GRID_DIM * MAX_GRID_DIM * MAX_GRID_DIM

        /**
         * Volume integral of the splat kernel `(1 - (r/R)^2)^2` over its support, divided by `R^3`.
         * Dividing by it is what makes each particle deposit exactly its own volume.
         */
        const val KERNEL_INTEGRAL = 0.957523f

        /** Ceiling on particle acceleration, in shell units per second squared. */
        const val MAX_ACCELERATION = 260f
    }
}
