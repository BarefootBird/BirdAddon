package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.SpiritBear
import com.barefootbird.birdaddon.utils.Debug
import com.barefootbird.birdaddon.utils.debugMessage
import com.odtheking.odin.events.MessageEvent.Chat
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object M4State {

    var bearTimer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    var timer = 0 // Times ticks from the start of boss

    val enteredRegex = Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$")

    // These track times in ticks for the events
    val bearSpawnTimes = mutableListOf<Int>()
    val bearKillTimes = mutableListOf<Int>()
    val bearSpawnStartTimes = mutableListOf<Int>()

    // Usually I just use odin's DungeonUtils for this, but odin's tick timer uses a different system, so I use this
    // to make sure that the odin timer and my timer match exactly
    var inThornBoss = false

    var ended = false

    // This spot is on the 0.85 blocks above the exact center of thorn, this is roughly the avg location of every bear spawn
    var bearSpawnSpot = Vec3(5.5, 69.85, 5.5)

    fun inBoss (): Boolean {
        return Debug.disableBossChecks || DungeonUtils.inBoss && DungeonUtils.isFloor(4)
    }

    fun updateBearSpawnSpot () {
        if (bearTimer == -1) {
            if (ParticleTrails.spawnPrediction.y != 0.0 && SpiritBear.experimental) {
                bearSpawnSpot = ParticleTrails.spawnPrediction
                return
            }
            M4Mobs.thorn?.let {
                // Bear spawns roughly on a circle around (5.5, 5.5) with a radius of 0.7,
                // and spawns on the closest point to where thorn was when the bear started spawning
                // also spawns roughly at 69.85y
                val dx = it.x - 5.5
                val dz = it.z - 5.5

                val angle = atan2(dz, dx)

                val r = 0.7
                val newX = 5.5 + r * cos(angle)
                val newZ = 5.5 + r * sin(angle)

                bearSpawnSpot = Vec3(newX, 69.85, newZ)
            }
        }
    }

    init {
        on<M4Event.BearSpawn> {
            bearSpawnTimes.add(timer)
        }

        on<M4Event.BearKill> {
            bearKillTimes.add(timer)
            bearTimer = -1
        }

        on<M4Event.BearSpawnStart> {
            bearTimer = 70
            bearSpawnStartTimes.add(timer)
        }

        on<Chat> {
            // Boss checks
            if (enteredRegex.matches(message)) {
                inThornBoss = true
            }
            if (!inThornBoss) return@on
        }

        on<TickEvent.Server> {
            if (!inThornBoss) return@on
            if (ended) return@on
            if (bearTimer > 0) {
                if (SpiritBear.experimental && ParticleTrails.predictionsRemaining > 0) {
                    bearTimer = ParticleTrails.predictionsRemaining - 1
                    debugMessage(ParticleTrails.predictionsRemaining)
                } else {
                    bearTimer--
                }
                if (!ParticleTrails.particleAddedThisTick && ParticleTrails.predictionsRemaining > -1) {
                    ParticleTrails.predictionsRemaining--
                }
                ParticleTrails.particleAddedThisTick = false
            }
            timer++
            updateBearSpawnSpot()
        }

        on<M4Event.End> {
            ended = true
        }

        on<LevelEvent.Load> {
            // Reset state on world load
            bearTimer = -1
            timer = 0
            bearKillTimes.clear()
            bearSpawnStartTimes.clear()
            bearSpawnTimes.clear()
            ended = false
            inThornBoss = false
        }
    }
}