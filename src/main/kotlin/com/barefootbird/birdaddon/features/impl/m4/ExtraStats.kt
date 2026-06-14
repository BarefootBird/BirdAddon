package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.toFixed
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
    private val extraStats by BooleanSetting("Extra Stats Menu", true, desc = "Shows the extra stats menu at the end of the run")

    private val printRunTime by BooleanSetting("Print Run Time", true, desc = "Prints run time in ticks to chat")
    private val printLastBearBreakdown by BooleanSetting("Print Last Bear Breakdown", true, desc = "Prints the last bear time in chat")

    data class M4RunSnapshot(
        val runId: UUID,
        val bearSpawnStartTimes: List<Int>,
        val bearSpawnTimes: List<Int>,
        val bearKillTimes: List<Int>,
        val lastBowReleased: Int,
        val lastBowHeldStart: Int,
        val timer: Int,
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

        if (bearId == run.bearKillTimes.size) { // last bear

            modMessage("Last bow shot ${(run.lastBowReleased - run.bearKillTimes[id]) / 20.0}s after bear died (held down for ${(run.lastBowReleased - run.lastBowHeldStart) / 20.0}s)")

            modMessage(
                "Thorn Died ${
                    ((run.timer - run.bearKillTimes[id]) / 20.0).toFixed(2)
                }s after last bear kill, ${
                    ((run.timer - run.bearSpawnTimes[id]) / 20.0).toFixed(2)
                }s after last bear spawn, ${
                    ((run.timer - run.lastBowReleased) / 20.0).toFixed(2)
                }s after bow release"
            )
        }
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
        on<M4Event.End> {
            val runId = UUID.randomUUID()
            val snapshot = M4RunSnapshot(
                runId = runId,
                bearSpawnStartTimes = M4State.bearSpawnStartTimes.toList(),
                bearSpawnTimes = M4State.bearSpawnTimes.toList(),
                bearKillTimes = M4State.bearKillTimes.toList(),
                lastBowReleased = M4State.lastSpiritBowRelease,
                lastBowHeldStart = M4State.lastSpiritBowHoldStart,
                timer = M4State.timer,
            )

            completedRuns[runId] = snapshot
            GlobalScope.launch {
                delay(1000)
                val lastBear = M4State.bearKillTimes.size - 1
                val killTime = M4State.bearKillTimes[lastBear]
                val spawnTime = M4State.bearSpawnTimes[lastBear]
                if (extraStats) {
                    sendBearStatsMenu(runId)
                }
                if (printRunTime) {
                    modMessage("Thorn Defeated in ${(M4State.timer / 20.0).toFixed(2)}s")
                }
                if (printLastBearBreakdown) {
                    modMessage("Spawn ${
                        spawnTime / 20.0
                    }s → Kill +${
                            (killTime - spawnTime) / 20.0
                    }s → Pickup +${
                        (M4State.lastSpiritBowPickup - killTime) / 20.0
                    }s → Hold +${
                        (M4State.lastSpiritBowHoldStart - M4State.lastSpiritBowPickup) / 20.0
                    }s → Release +${
                        (M4State.lastSpiritBowRelease - M4State.lastSpiritBowHoldStart) / 20.0
                    }s → End +${
                        (M4State.timer - M4State.lastSpiritBowRelease) / 20.0
                    }s (${
                        (M4State.timer - spawnTime) / 20.0
                    }s after spawn)")
                }
            }
        }
    }
}