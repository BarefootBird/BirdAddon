package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.Webserver.startWebserver
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.events.WorldEvent
import net.minecraft.client.Minecraft
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Logging: Module(
    name = "Logging",
    description = "Logging for m4 <3",
    category = Category.M4
) {
    private val logs = mutableListOf<List<String>>()
    private var ended = false // prevents logging multiple times
    private val startWebserver by ActionSetting("Start Webserver", "Starts the m4 webserver website thingy") {
        startWebserver()
    }

    init {
        on<TickEvent.Server> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(4)) return@on
            if (M4State.timer < 300) return@on
            logs.add(
                listOf(
                    M4State.kills.toString(), M4State.timer.toString(),
                    M4Mobs.bats.size.toString(), M4Mobs.chickens.size.toString(), M4Mobs.rabbits.size.toString(),
                    M4Mobs.sheep.size.toString(), M4Mobs.cows.size.toString(), M4Mobs.wolves.size.toString(),
                    M4Mobs.totalBats.size.toString(), M4Mobs.totalChickens.size.toString(), M4Mobs.totalRabbits.size.toString(),
                    M4Mobs.totalSheep.size.toString(), M4Mobs.totalCows.size.toString(), M4Mobs.totalWolves.size.toString()
                    )
            )
        }

        on<WorldEvent.Load> {
            logs.clear()
            ended = false
        }


        on<ChatPacketEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            if (M4State.endRegex.matches(value) && !ended) {
                ended = true

                val mc: Minecraft = Minecraft.getInstance()
                val logsFolder = File(mc.gameDirectory, "m4logs/logs").apply { mkdirs() }
                val timestamp = Instant.now().toEpochMilli()
                val target = File(logsFolder, "$timestamp.csv").apply { createNewFile() }
                val logText = StringBuilder()

                val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy h:mm:ss a")

                logText.append("Thorn Fight Log - ${
                    Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime().format(formatter)}\n")

                logText.append(
                    "Bear Spawning Start Times: ${
                        M4State.bearSpawnStartTimes.joinToString(", ") { (it / 20.0).toString() }
                    }\n"
                )

                logText.append(
                    "Bear Spawn Times: ${
                        M4State.bearSpawnTimes.joinToString(", ") { (it / 20.0).toString() }
                    }\n"
                )

                logText.append(
                    "Bear Kill Times: ${
                        M4State.bearKillTimes.joinToString(", ") { (it / 20.0).toString() }
                    }\n"
                )

                logText.append("Master Mode: ${DungeonUtils.floor?.isMM}\n")

                logText.append("Party:\n")
                DungeonUtils.dungeonTeammates.forEach { player ->
                    logText.append("${player.name}, ${player.clazz}\n")
                }

                logText.append("\n")

                logText.append("Kills,TimerTicks,Bats,Chickens,Rabbits,Sheep,Cows,Wolves,TotalBats,TotalChickens,TotalRabbits,TotalSheep,TotalCows,TotalWolves\n")

                logs.forEach { entry ->
                    logText.append(entry.joinToString(",") + "\n")
                }
                target.writeText(logText.toString())
            }
        }
    }
}