package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.state.BowTracker
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.state.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.barefootbird.birdaddon.utils.state.BearTracker
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DelicateCoroutinesApi::class)
object ExtraStats: Module(
    name = "Extra Stats",
    key = null,
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
        val bowReleased: Int?,
        val bowDrawn: Int?,
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

            if (run.bowReleased != null && run.bowDrawn != null) {
                modMessage("Last bow shot ${(run.bowDrawn - run.bearKillTimes[id]) / 20.0}s after bear died (held down for ${(run.bowReleased - run.bowDrawn) / 20.0}s)")
            }

            if (run.bowReleased != null) {
                modMessage(
                    "Thorn Died ${
                        ((run.timer - run.bearKillTimes[id]) / 20.0).toFixed(2)
                    }s after last bear kill, ${
                        ((run.timer - run.bearSpawnTimes[id]) / 20.0).toFixed(2)
                    }s after last bear spawn, ${
                        ((run.timer - run.bowReleased) / 20.0).toFixed(2)
                    }s after bow release"
                )
            }
        }
    }


    // The menu that's sent after the run
    fun sendBearStatsMenu(runId: UUID) {

        val message = Component.literal("Click to view stats: ")
            .withStyle(ChatFormatting.YELLOW)

        for (i in 1..BearTracker.bearKillTimes.size) {
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

            if (i < BearTracker.bearKillTimes.size) {
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
                bearSpawnStartTimes = BearTracker.bearSpawnStartTimes.toList(),
                bearSpawnTimes = BearTracker.bearSpawnTimes.toList(),
                bearKillTimes = BearTracker.bearKillTimes.toList(),
                bowReleased = BowTracker.snapshot.release,
                bowDrawn = BowTracker.snapshot.draw,
                timer = M4State.timer,
            )

            completedRuns[runId] = snapshot
            GlobalScope.launch {
                delay(1000.milliseconds)
                val lastBear = BearTracker.bearKillTimes.size - 1
                val killTime = BearTracker.bearKillTimes[lastBear]
                val spawnTime = BearTracker.bearSpawnTimes[lastBear]
                if (extraStats) {
                    sendBearStatsMenu(runId)
                }
                if (printRunTime) {
                    modMessage("Thorn Defeated in ${(M4State.timer / 20.0).toFixed(2)}s")
                }
                if (printLastBearBreakdown) {
                    val pickup = BowTracker.snapshot.pickup
                    val draw = BowTracker.snapshot.draw
                    val release = BowTracker.snapshot.release
                    if (pickup != null && draw != null && release != null)
                    modMessage("Spawn ${
                        spawnTime / 20.0
                    }s → Kill +${
                            (killTime - spawnTime) / 20.0
                    }s → Pickup +${
                        (pickup - killTime) / 20.0
                    }s → Hold +${
                        (draw - pickup) / 20.0
                    }s → Release +${
                        (release - draw) / 20.0
                    }s → End +${
                        (M4State.timer - release) / 20.0
                    }s (${
                        (M4State.timer - spawnTime) / 20.0
                    }s after spawn)")
                }
            }
        }
    }
}
