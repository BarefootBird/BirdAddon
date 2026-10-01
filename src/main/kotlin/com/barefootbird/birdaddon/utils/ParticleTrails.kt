package com.barefootbird.birdaddon.utils

import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.roundToInt

object ParticleTrails {

    data class TimedParticle(
        val pos: Vec3,
        val createdTick: Int,
        var linearPortion: Boolean = false,
        var prediction: Boolean = false
    )

    var predictionsRemaining = -1
    var particleAddedThisTick = false

    private const val TARGET_X = 5.5
    private const val TARGET_Z = 5.5
    private const val TARGET_Y = 69.0
    private const val EPSILON = 0.0001
    private const val EPSILON_SQ = EPSILON * EPSILON

    // Particles trails have a linear portion where the particles head towards (5.5, 69.0, 5.5)
    // This function checks if 2 particles are within a certain tolerance (epsilon) of that line
    private fun isInLinearPortion(a: Vec3, b: Vec3): Boolean {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z

        val lengthSq = dx * dx + dz * dz + dy * dy
        if (lengthSq == 0.0) return false

        val tx = TARGET_X - a.x
        val tz = TARGET_Z - a.z
        val ty = TARGET_Y - a.y

        val crossX = dy * tz - dz * ty
        val crossY = dz * tx - dx * tz
        val crossZ = dx * ty - dy * tx

        val crossLengthSq =
            crossX * crossX +
                    crossY * crossY +
                    crossZ * crossZ

        return crossLengthSq <= EPSILON_SQ * lengthSq
    }


    val particles = mutableListOf<TimedParticle>()

    var spawnPrediction = Vec3(0.0, 0.0, 0.0)

    /*
    * if there are missing particles between p1 and p2 that didn't spawn due to
    * particle cap or render distance or whatever else then this scales it accordingly
    */
    fun getParticleStep(delta: Vec3): Vec3? {

        /*
        * When the particles spawn, the usual difference in y values of particles that are next to each other
        * Varies but falls somewhere within the bounds of 0.36 and 0.4 so 0.38 is very roughly the avg y delta
        */
        val spacing = 0.38

        val particleSteps = (kotlin.math.abs(delta.y) / spacing).roundToInt()
            .coerceAtLeast(1)

        val perSegmentY = kotlin.math.abs(delta.y) / particleSteps

        // Make sure that the result is within the expected bounds
        return if (perSegmentY in 0.36..0.4) {
            delta.scale(1.0 / particleSteps)
        } else {
            null
        }
    }

    private fun quantizeToGrid(pos: Vec3): Vec3 =
        Vec3(
            floor(pos.x * 32) / 32,
            floor(pos.y * 32) / 32,
            floor(pos.z * 32) / 32
        )

    fun generatePredictions(): List<TimedParticle> {
        val linear = particles.filter { it.linearPortion }
        if (linear.size < 2) return emptyList()


        val first = linear[0].pos
        val second = linear[1].pos

        val lower = if (first.y < second.y) first else second
        val upper = if (first.y < second.y) second else first

        // Get a vector pointing from the upper particle to lower
        val delta = lower.subtract(upper)

        // Scale the delta to account for missing particles due to server being weird
        val step = getParticleStep(delta) ?: return emptyList()

        // The actual lower bound of how low particles can spawn is not a constant and does vary somewhat
        // But it is generally between 69.695 and 69.697
        // I have no idea what the actual function for the lower bound is or how to find it
        val particleLowerBound = 69.6969

        // Start projecting from the lower particle to create a sequence of predicted particles
        val predictions = generateSequence(lower.add(step)) { it.add(step) }
            .takeWhile { it.y > particleLowerBound }
            .map { TimedParticle(it, M4State.timer) }
            .toList()

        if (predictions.isEmpty()) {
            return emptyList()
        }

        // The last particle quantized on to a 1/32 grid is where the bear spawns
        spawnPrediction = quantizeToGrid(predictions.last().pos)

        return predictions
    }

    // Returns how many more particles there are predicted to be based on the position of the current particle
    fun getPredictionsRemaining(pos: Vec3): Int {
        // Find the prediction that represents the current particle position
        val closestPrediction = predictions.minByOrNull { it.pos.distanceToSqr(pos) } ?: return -1

        return predictions.size - predictions.indexOf(closestPrediction)
    }

    private fun isPotentialBearParticle(particle: ParticleOptions): Boolean {
        val dustParticle = particle as? DustParticleOptions ?: return false

        val col = dustParticle.color

        val bearDustColorComponent = 9.804E-2f

        return col.x != bearDustColorComponent ||
                col.y != bearDustColorComponent ||
                col.z != bearDustColorComponent
    }

    var predictions = emptyList<TimedParticle>()

    init {
        on<LevelEvent.Load> {
            predictions = emptyList()
            particles.clear()
        }

        on<TickEvent.Server> {
            if (!M4State.inBoss()) return@on
            val cutoff = M4State.timer - 8

            particles.removeIf { it.createdTick < cutoff }

            if (M4State.bearSpawnTimes.isNotEmpty()) {
                if (M4State.timer == M4State.bearSpawnTimes.last() + 3) {
                    predictions = emptyList()
                    predictionsRemaining = -1
                    spawnPrediction = Vec3(0.0, 0.0, 0.0)
                }
            }
        }

        onReceive<ClientboundLevelParticlesPacket> { event ->
            if (!M4State.inBoss()) return@onReceive
            if (M4State.bearSpawnStartTimes.size <= M4State.bearSpawnTimes.size) return@onReceive // Only need to worry about particles while the bear is spawning
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@onReceive
            if (!isPotentialBearParticle(packet.particle)) return@onReceive

            val newParticle = TimedParticle(
                Vec3(packet.x, packet.y, packet.z),
                M4State.timer
            )

            for (existing in particles) {
                if (isInLinearPortion(existing.pos, newParticle.pos)) {

                    existing.linearPortion = true
                    newParticle.linearPortion = true

                    particleAddedThisTick = true
                    // Use the lower particle for the predictions remaining
                    val remaining = if (newParticle.pos.y < existing.pos.y) {
                        getPredictionsRemaining(newParticle.pos)
                    } else {
                        getPredictionsRemaining(existing.pos)
                    }
                    // Bear timer should never go up once predictions are found
                    if (predictionsRemaining !in 0..remaining) {
                        predictionsRemaining = remaining
                    }

                    break
                }
            }

            particles += newParticle

            if (predictions.isEmpty() && newParticle.linearPortion) {
                predictions = generatePredictions()
            }
        }
    }
}