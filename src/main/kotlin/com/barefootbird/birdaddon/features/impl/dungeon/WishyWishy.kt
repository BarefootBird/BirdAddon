package com.barefootbird.birdaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket


object WishyWishy: Module(
    name = "Wishy Wishy",
    description = "Wish helper for m4"
) {
    // Credit to Devonian for some of this code <3
    private val autoWish by BooleanSetting("Auto Wish", true, desc = "Automatically uses wish when tank mastiff swaps")
    private var wishSent = false
    private val teamRegex = "^team_(\\d+)$".toRegex()

    private var tankInMastiff = false


    init {

        on<TickEvent.End> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on
            if (!wishSent && tankInMastiff) {
                wishSent = true
                if (DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.Healer) {
                    if (autoWish) {
                        mc.player?.drop(false)
                    }
                }
            }
        }

        onReceive<ClientboundSetPlayerTeamPacket> { event ->
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@onReceive
            val packet = event.packet
            if (packet is ClientboundSetPlayerTeamPacket) {
                val opt = packet.parameters
                if (!opt.isPresent) return@onReceive
                val team = opt.get()
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return@onReceive
                if (!packet.name.matches(teamRegex)) return@onReceive
                val message = "${teamPrefix}${teamSuffix.trim()}".noControlCodes
                if (!message.contains("[T]")) return@onReceive
                val health = Regex("""\d+""").find(message.replace(",", ""))?.value?.toIntOrNull() ?: return@onReceive
                tankInMastiff = health >= 100000
            }
        }


        on<WorldEvent.Load> {
            wishSent = false
            tankInMastiff = false
        }

    }
}