package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import net.minecraft.world.InteractionHand


object BowTracker {
    data class BowSnapshot(
        val pickup: Int? = null,
        val draw: Int? = null,
        val release: Int? = null,
    )

    private var state = BowSnapshot()

    val snapshot: BowSnapshot
        get() = state

    private var drawn = false

    private fun updateSpiritBowData () {

        val playersSnapshot = mc.level!!.players().toList()

        if (playersSnapshot.any { it.getItemInHand(InteractionHand.MAIN_HAND).displayName.string == "[Spirit Bow]" && it.isUsingItem }) {
            if (!drawn) {
                state = state.copy(draw = Timers.timer)
            }
            drawn = true
        } else {
            if (drawn) {
                state = state.copy(release = Timers.timer - 1)
            }
            drawn = false
        }
    }

    init {
        on<M4Event.BowPickup> {
            state = state.copy(pickup = Timers.timer)
        }

        on<TickEvent.Server> {
            updateSpiritBowData()
        }

        on<LevelEvent.Load> {
            state = state.copy(pickup = null, draw = null, release = null)
        }
    }
}