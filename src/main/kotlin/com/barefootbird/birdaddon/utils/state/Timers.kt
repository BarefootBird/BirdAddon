package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.SpiritBear
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on

object Timers {

    var bearTimer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    var timer = 0 // Times ticks from the start of boss

    init {

        on<M4Event.BearKill> {
            bearTimer = -1
        }

        on<M4Event.BearSpawnStart> {
            bearTimer = 70
        }

        on<TickEvent.Server> {
            // Odin splits start on dialogue start so this is needed for the splits to match with Odin's
            if (!BossLifecycle.thornDialogueStarted) return@on
            if (BossLifecycle.ended) return@on
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

        on<LevelEvent.Load> {
            bearTimer = -1
            timer = 0
        }
    }
}