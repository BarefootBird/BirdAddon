package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Debug
import com.barefootbird.birdaddon.utils.state.Timers.bearTimer
import com.barefootbird.birdaddon.utils.state.Timers.timer
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object BossLifecycle {
    var thornDialogueStarted = false

    val enteredRegex = Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$")


    var ended = false

    fun inBoss(): Boolean {
        return Debug.disableBossChecks || (DungeonUtils.inBoss && DungeonUtils.isFloor(4))
    }
    init {
        on<MessageEvent.Chat> {
            // Boss checks
            if (enteredRegex.matches(message)) {
                thornDialogueStarted = true
            }
            if (!thornDialogueStarted) return@on
        }

        on<M4Event.End> {
            ended = true
        }

        on<LevelEvent.Load> {
            // Reset state on world load
            bearTimer = -1
            timer = 0
            ended = false
            thornDialogueStarted = false
        }


    }
}