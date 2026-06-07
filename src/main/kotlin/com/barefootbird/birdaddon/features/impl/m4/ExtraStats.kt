package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import java.util.UUID

@OptIn(DelicateCoroutinesApi::class)
object ExtraStats: Module(
    name = "Extra Stats",
    description = "Shows stats from your run",
    category = Category.M4
) {
    private var ended = false


    data class M4RunSnapshot(
        val runId: UUID,
        val bearSpawnStartTimes: List<Int>,
        val bearSpawnTimes: List<Int>,
        val bearKillTimes: List<Int>
    )

    val completedRuns = mutableMapOf<UUID, M4RunSnapshot>()

    // shows stats for a specified bear
    fun showBearStats(runId: String, bearId: Int) {
        val run = completedRuns[UUID.fromString(runId)] ?: return
        val id = bearId - 1 // change from 1 indexed to 0 indexed
        modMessage("Bear $bearId Stats:")
        modMessage("Spawn Start: ${run.bearSpawnStartTimes[id] / 20.0}s")
        modMessage("Bear spawned: ${run.bearSpawnTimes[id] / 20.0}s")
        modMessage("Bear Killed: ${run.bearKillTimes[id] / 20.0}s (took ${(run.bearKillTimes[id] - run.bearSpawnTimes[id]) / 20.0}s)")
    }


    // The menu that's sent after the run
    fun sendBearStatsMenu(runId: UUID) {

        val message = Component.literal("Click to view stats: ")
            .withStyle(ChatFormatting.YELLOW)

        for (i in 1..M4State.bearKillTimes.size) {
            val bearText = Component.literal("Bear $i")
                .withStyle {
                    it.withColor(ChatFormatting.GOLD)
                        .withClickEvent(
                            ClickEvent.RunCommand("/bearstats $runId $i")
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
            if (!M4State.inBoss()) return@on
            if (M4State.endRegex.matches(value) && !ended) {
                val runId = UUID.randomUUID()
                ended = true
                val snapshot = M4RunSnapshot(
                    runId = runId,
                    bearSpawnStartTimes = M4State.bearSpawnStartTimes.toList(),
                    bearSpawnTimes = M4State.bearSpawnTimes.toList(),
                    bearKillTimes = M4State.bearKillTimes.toList()
                )

                completedRuns[runId] = snapshot
                GlobalScope.launch {
                    delay(1000)
                    sendBearStatsMenu(runId)
                }
            }
        }

        on<WorldEvent.Load> {
            ended = false
        }
    }
}