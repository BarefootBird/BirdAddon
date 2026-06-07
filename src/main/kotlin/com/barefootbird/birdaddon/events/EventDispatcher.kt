package com.barefootbird.birdaddon.events

import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on

object EventDispatcher {

    val bearSpawnRegex = Regex("^A Spirit Bear has appeared!$")
    val bearKillRegex = Regex("^The Spirit Bow has dropped!$")
    val endRegex = Regex("^\\s*☠ Defeated Thorn in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    var ended = false

    // Callback so that the somewhat messy bear spawn start logic can stay in M4State
    fun triggerBearSpawnStart() {
        if (M4State.bearTimer == -1) {
            M4Event.BearSpawnStart().postAndCatch()
        }
    }

    init {
        on<ChatPacketEvent> {
            if (!M4State.inBoss()) return@on

            if (bearSpawnRegex.matches(value)) {
                M4Event.BearSpawn().postAndCatch()
            }
            if (bearKillRegex.matches(value)) {
                if (M4State.bearTimer != -1) {
                    M4Event.BearKill().postAndCatch()
                }
            }
            if (endRegex.matches(value) && !ended) {
                ended = true
                M4Event.End().postAndCatch()
            }
        }

        on<WorldEvent.Load> {
            ended = false
        }
    }
}