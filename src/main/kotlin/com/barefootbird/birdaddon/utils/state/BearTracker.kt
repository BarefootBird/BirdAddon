package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.SpiritBear
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BearTracker {

    // These track times in ticks for the events
    val spawnTimes = mutableListOf<Int>()
    val killTimes = mutableListOf<Int>()
    val spawnStartTimes = mutableListOf<Int>()

    // This spot is on the 0.85 blocks above the exact center of m4, this is roughly the avg location of every bear spawn
    var spawnSpot = Vec3(5.5, 69.85, 5.5)

    fun spawning (): Boolean {
        return spawnStartTimes.size > spawnTimes.size
    }

    fun spawned (): Boolean {
        return spawnTimes.size > killTimes.size
    }

    fun updateBearSpawnSpot () {
        if (spawning()) {
            if (ParticleTrails.spawnPrediction.y != 0.0 && SpiritBear.experimental) {
                spawnSpot = ParticleTrails.spawnPrediction
                return
            }
            Mobs.thorn?.let {
                // Bear spawns roughly on a circle around (5.5, 5.5) with a radius of 0.7,
                // and spawns on the closest point to where thorn was when the bear started spawning
                // also spawns roughly at 69.85y
                val dx = it.x - 5.5
                val dz = it.z - 5.5

                val angle = atan2(dz, dx)

                val r = 0.7
                val newX = 5.5 + r * cos(angle)
                val newZ = 5.5 + r * sin(angle)

                spawnSpot = Vec3(newX, 69.85, newZ)
            }
        }
    }

    init {
        on<M4Event.BearSpawn> {
            spawnTimes.add(Timers.timer)
        }

        on<M4Event.BearKill> {
            killTimes.add(Timers.timer)
        }

        on<M4Event.BearSpawnStart> {
            spawnStartTimes.add(Timers.timer)
        }

        on<LevelEvent.Load> {
            killTimes.clear()
            spawnStartTimes.clear()
            spawnTimes.clear()
        }

        on<TickEvent.Server> {
            updateBearSpawnSpot()
        }
    }
}