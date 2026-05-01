package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object MobCounters: Module(
    name = "Mob Counter",
    description = "Shows how many mobs are alive in m4",
    category = Category.M4
) {
    private val hud by HUD(name, "Mob Counter Hud", false) { example ->
        if (example) {
            return@HUD textDim("Total Mobs: 100", 0, 0, Colors.WHITE)
        }
        if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(4)) {
            return@HUD 0 to 0
        }

        val features = listOf(totalGUI, cowsGUI, sheepGUI, chickensGUI, batsGUI, wolvesGUI, rabbitsGUI, rabbitsUnderThornGui)

        runCatching {
            features.forEachIndexed { index, enabled ->
                if (!enabled) return@forEachIndexed
                val displayText = when (index) {
                    0 -> "Mobs: " +
                            (M4Mobs.cows.size +
                                    M4Mobs.sheep.size +
                                    M4Mobs.chickens.size +
                                    M4Mobs.bats.size +
                                    M4Mobs.wolves.size +
                                    M4Mobs.rabbits.size)

                    1 -> "Cows: " + M4Mobs.cows.size
                    2 -> "Sheep: " + M4Mobs.sheep.size
                    3 -> "Chickens: " + M4Mobs.chickens.size
                    4 -> "Bats: " + M4Mobs.bats.size
                    5 -> "Wolves: " + M4Mobs.wolves.size
                    6 -> "Rabbits: " + M4Mobs.rabbits.size
                    7 -> "Rabbits under Thorn: " + M4Mobs.rabbits.count { it.x > 21 && it.z > 21 }
                    else -> ""
                }
                textDim(displayText, 0, (index - 1) * 9, Colors.WHITE)
            }
        }
        "Rabbits Under Thorn: 00".length to 9 * (features.count { it })


    }

    private val totalGUI by BooleanSetting("Total Mobs", true, desc = "Show Total Mobs")
    private val cowsGUI by BooleanSetting("Cows", true, desc = "Shows alive cows")
    private val sheepGUI by BooleanSetting("Sheep", true, desc = "Shows alive sheep")
    private val chickensGUI by BooleanSetting("Chickens", true, desc = "Shows alive chickens")
    private val batsGUI by BooleanSetting("Bats", true, desc = "Shows alive bats")
    private val wolvesGUI by BooleanSetting("Wolves", true, desc = "Shows alive wolves")
    private val rabbitsGUI by BooleanSetting("Rabbits", true, desc = "Shows alive rabbits")
    private val rabbitsUnderThornGui by BooleanSetting("Rabbits Under Thorn", default = true, desc = "Shows rabbits under thorn")
}