package com.barefootbird.birdaddon.utils

import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

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

    fun generatePredictions(): List<TimedParticle> {
        val linear = particles.filter { it.linearPortion }

        val predictions = mutableListOf<TimedParticle>()

        if (linear.size >= 2) {
            val p1 = linear[0].pos
            val p2 = linear[1].pos

            var d = p2.subtract(p1)

            if (d.y >= 0) {
                d = p1.subtract(p2)
            }

            var scale = 1.0

            for (i in 1..100) {
                // rough range of normal y variation between particles
                if (d.y/i in -0.4..-0.36) {
                    scale = 1.0/i
                    break
                }
            }

            /*
            * if there are missing particles between p1 and p2 that didn't spawn due to
            * particle cap or render distance or whatever else then this scales it accordingly
            */
            d = d.scale(scale)

            // start projecting from the lower particle
            var newPos = if (p1.y < p2.y) {
                p1.add(d)
            } else {
                p2.add(d)
            }


            while (newPos.y > 69.6969) {
                predictions.add(TimedParticle(newPos, M4State.timer))
                newPos = newPos.add(d)
            }

            if (predictions.isEmpty()) {
                return emptyList()
            }

            val last = predictions.last().pos

            val spawnPrediction = Vec3(floor(last.x * 32) / 32, floor(last.y * 32) / 32, floor(last.z * 32) / 32)

            ParticleTrails.spawnPrediction = spawnPrediction

            return predictions
        }
        return emptyList()
    }

    fun getPredictionsRemaining(pos: Vec3): Int {
        if (predictions.isEmpty()) return -1

        var closestIndex = 0
        var closestDistance = Double.MAX_VALUE

        for (i in predictions.indices) {
            val distance = predictions[i].pos.distanceToSqr(pos)

            if (distance < closestDistance) {
                closestDistance = distance
                closestIndex = i
            }
        }

        return predictions.size - closestIndex - 1
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

            if (!M4State.bearSpawnTimes.isEmpty()) {
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
            val packet = event.packet
            if (packet is ClientboundLevelParticlesPacket) {
                val type = BuiltInRegistries.PARTICLE_TYPE.getKey(packet.particle.type)

                if (type.toString() == "minecraft:dust") {
                    val col = (packet.particle as? DustParticleOptions)?.color

                    val bearParticleColor = "( 9.804E-2  9.804E-2  9.804E-2)"

                    if (col.toString() != bearParticleColor) {
                        return@onReceive
                    }

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
    }
}