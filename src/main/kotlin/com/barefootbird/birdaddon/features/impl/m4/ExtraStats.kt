package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

@OptIn(DelicateCoroutinesApi::class)
object ExtraStats: Module(
    name = "Extra Stats",
    description = "Shows stats from your run",
    category = Category.M4
) {
    private var ended = false

    // shows stats for a specified bear
    fun showBearStats(bearId: Int) {
        val id = bearId - 1 // change from 1 indexed to 0 indexed
        modMessage("Spawn Start: ${M4State.bearSpawnStartTimes[id] * 20}s")
        modMessage("Bear spawned: ${M4State.bearSpawnTimes[id] * 20}s")
        modMessage("Bear Killed: ${M4State.bearKillTimes[id] * 20}s (took ${(M4State.bearKillTimes[id] - M4State.bearSpawnTimes[id]) * 20}s)")
    }

    // The menu that's sent after the run
    fun sendBearStatsMenu() {
        val message = Component.literal("Click to view stats: ")
            .withStyle(ChatFormatting.YELLOW)

        for (i in 1..M4State.bearKillTimes.size) {
            val bearText = Component.literal("Bear $i")
                .withStyle {
                    it.withColor(ChatFormatting.GOLD)
                        .withClickEvent(
                            ClickEvent.RunCommand("/bearstats $i")
                        )
                        .withHoverEvent(
                            HoverEvent.ShowText(
                                Component.literal("Click to view Bear $i stats")
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                        .withUnderlined(true)
                }

            message.append(bearText)

            if (i < M4State.bearKillTimes.size) {
                message.append(
                    Component.literal(" | ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                )
            }
        }

        modMessage(message)
    }

    init {

        on<ChatPacketEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on
            if (M4State.endRegex.matches(value) && !ended) {
                ended = true
                GlobalScope.launch {
                    delay(1000)
                    sendBearStatsMenu()
                }
            }
        }

        on<WorldEvent.Load> {
            ended = false
        }
    }
}