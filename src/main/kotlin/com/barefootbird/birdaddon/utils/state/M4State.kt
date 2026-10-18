package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.SpiritBear
import com.barefootbird.birdaddon.utils.Debug
import com.odtheking.odin.events.MessageEvent.Chat
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object M4State {

    var bearTimer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    var timer = 0 // Times ticks from the start of boss

    val enteredRegex = Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$")

    // Usually I just use odin's DungeonUtils for this, but odin's splits uses the boss message to start the timer
    // so I use this to make sure that the odin timer and my timer match exactly
    var inThornBoss = false

    var ended = false


    fun inBoss (): Boolean {
        return Debug.disableBossChecks || DungeonUtils.inBoss && DungeonUtils.isFloor(4)
    }

    init {

        on<M4Event.BearKill> {
            bearTimer = -1
        }

        on<M4Event.BearSpawnStart> {
            bearTimer = 70
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
            if (BearTracker.bearSpawning()) {
                if (SpiritBear.experimental && ParticleTrails.predictionsRemaining > 0) {
                    bearTimer = ParticleTrails.predictionsRemaining - 1
                } else {
                    bearTimer--
                }
                if (!ParticleTrails.particleAddedThisTick && ParticleTrails.predictionsRemaining > -1) {
                    ParticleTrails.predictionsRemaining--
                }
                ParticleTrails.particleAddedThisTick = false
            }
            timer++
        }

        on<M4Event.End> {
            ended = true
        }

        on<LevelEvent.Load> {
            // Reset state on world load
            bearTimer = -1
            timer = 0
            ended = false
            inThornBoss = false
        }
    }
}