package com.barefootbird.birdaddon.events

import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.debugMessage
import com.odtheking.odin.events.ChatMessageEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket

object EventDispatcher {

    val bearSpawnRegex = Regex("^A Spirit Bear has appeared!$")
    val bearKillRegex = Regex("^The Spirit Bow has dropped!$")
    val bowPickupRegex = Regex("^.* picked up the Spirit Bow!$")
    val endRegex = Regex("^\\s*☠ Defeated Thorn in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    var ended = false

    // Callback so that the somewhat messy bear spawn start logic can stay in M4State
    fun triggerBearSpawnStart() {
        if (M4State.bearTimer == -1) {
            M4Event.BearSpawnStart().postAndCatch()
        }
    }

    init {
        on<ChatMessageEvent> {
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

        onReceive<ClientboundSetSubtitleTextPacket> {
            val packet = it.packet
            if (packet is ClientboundSetSubtitleTextPacket) {
                if (packet.text.string.noControlCodes.matches(bowPickupRegex)) {
                    M4Event.BowPickup().postAndCatch()
                }
            }
        }

        on<LevelEvent.Load> {
            ended = false
        }
    }
}