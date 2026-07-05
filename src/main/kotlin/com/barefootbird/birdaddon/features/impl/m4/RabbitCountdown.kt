package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed

object RabbitCountdown: Module(
    name = "Rabbit Countdown",
    description = "Counts down when rabbits spawn",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays the time until rabbits spawn in HUD", false) { example ->
        val lines = displayLines(example)
        var width = 0
        var height = 0
        lines.forEachIndexed { index, line ->
            val (lineWidth, lineHeight) = textDim(line, 0, index * 9, Colors.WHITE)
            width = maxOf(width, lineWidth)
            height = (index * 9) + lineHeight
        }
        width to height
    }

    private val decimals by NumberSetting("Decimals", 2, 1, 2, 1, "How many decimals to show")

    private val showFromStart by BooleanSetting("Show on boss start", true, "Shows the timer from start of boss")
    private val showAfterB1 by BooleanSetting("Show after b1 start", true, "Shows the timer only after b1 starts spawning").withDependency { !showFromStart }
    private val secondsBefore by NumberSetting("Seconds before", 4, 2, 7, 1, "How many seconds before rabbits spawn to render the timer").withDependency {
        !showFromStart && !showAfterB1
    }

    private val hideWhen0 by BooleanSetting("Hide counters when 0", true, "Hides the mob counters when there are 0 of that mob")

    private val displayText by StringSetting(
        "Display text:",
        $$"Rabbits spawning in",
        desc = $$"HUD format for when rabbits are about to spawn"
    )

    private val showArenaMobs by BooleanSetting(
        "Show Ground Mobs",
        true,
        desc = "Shows alive mobs that are on the ground"
    )

    private val showBats by BooleanSetting(
        "Show Bats",
        true,
        desc = "Shows alive bats"
    )

    private const val RABBIT_SPAWN_TIME = 710

    private fun displayLines(example: Boolean): List<String> {
        if (example) {
            return listOf(
                "§b$displayText 35.5s",
                "§dGround: 3",
                "§fBats: 1"
            )
        }

        if (!M4State.inBoss() || DungeonUtils.floor?.isMM != true) return emptyList()

        val lines = mutableListOf<String>()
        val ticksUntilRabbitSpawn = RABBIT_SPAWN_TIME - M4State.timer
        if (ticksUntilRabbitSpawn <= 0) return emptyList()

        val shouldShowTimer = (showFromStart || (ticksUntilRabbitSpawn <= secondsBefore * 20 && !showAfterB1) || (showAfterB1 && M4State.bearSpawnStartTimes.isNotEmpty()))
        if (!shouldShowTimer) {
            return emptyList()
        }

        lines.add("§b$displayText ${(ticksUntilRabbitSpawn / 20.0).toFixed(decimals)}s")

        if (showArenaMobs && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.HEALER) {
            val groundMobs = M4Mobs.cows.size +
                    M4Mobs.sheep.size +
                    M4Mobs.chickens.size +
                    M4Mobs.wolves.size +
                    M4Mobs.rabbits.size

            if (groundMobs > 0 || !hideWhen0) {
                lines.add("§dGround: $groundMobs")
            }
        }

        if (showBats && DungeonUtils.currentDungeonPlayer.clazz in setOf(DungeonClass.BERSERK, DungeonClass.MAGE)) {
            val bats = M4Mobs.bats.size
            if (bats > 0 || !hideWhen0) {
                lines.add("§fBats: $bats")
            }

        }

        return lines
    }
}
