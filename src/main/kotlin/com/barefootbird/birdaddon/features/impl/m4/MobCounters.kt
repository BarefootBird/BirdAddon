package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
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
        val features = listOf(totalGUI, cowsGUI, sheepGUI, chickensGUI, batsGUI, wolvesGUI, rabbitsGUI, mobsUnderThornGui)

        var enabledFeatures = 0

        fun isUnderThorn(x: Double, y: Double, z: Double): Boolean {
            return (y < 75 && (x > 21 && z > 21)
                    || (x > 26 && z > 16)
                    || (x > 16 && z > 26))
        }

        val allMobs = listOf(
            M4Mobs.cows,
            M4Mobs.sheep,
            M4Mobs.chickens,
            M4Mobs.bats,
            M4Mobs.wolves,
            M4Mobs.rabbits
        )

        runCatching {
            features.forEachIndexed { index, enabled ->
                if (!enabled) return@forEachIndexed
                enabledFeatures++
                val displayText = when (index) {
                    0 -> totalMobsPrefix + allMobs.sumOf { it.size }

                    1 -> cowsPrefix + M4Mobs.cows.size
                    2 -> sheepPrefix + M4Mobs.sheep.size
                    3 -> chickensPrefix + M4Mobs.chickens.size
                    4 -> batsPrefix + M4Mobs.bats.size
                    5 -> wolvesPrefix + M4Mobs.wolves.size
                    6 -> rabbitsPrefix + M4Mobs.rabbits.size
                    7 -> mobsUnderThornPrefix + allMobs.sumOf { mobList ->
                        mobList.count { mob ->
                            isUnderThorn(mob.x, mob.y, mob.z)
                        }
                    }
                    else -> ""
                }
                textDim(displayText, 0, (enabledFeatures - 1) * 9, Colors.WHITE)
            }
        }
        "Rabbits Under Thorn: 00".length to 9 * enabledFeatures
    }

    private val totalGUI by BooleanSetting("Total Mobs", true, desc = "Show Total Mobs")
    private val cowsGUI by BooleanSetting("Cows", true, desc = "Shows alive cows")
    private val sheepGUI by BooleanSetting("Sheep", true, desc = "Shows alive sheep")
    private val chickensGUI by BooleanSetting("Chickens", true, desc = "Shows alive chickens")
    private val batsGUI by BooleanSetting("Bats", true, desc = "Shows alive bats")
    private val wolvesGUI by BooleanSetting("Wolves", true, desc = "Shows alive wolves")
    private val rabbitsGUI by BooleanSetting("Rabbits", true, desc = "Shows alive rabbits")
    private val mobsUnderThornGui by BooleanSetting("Mobs Under Thorn", default = true, desc = "Shows mobs under thorn")

    private val totalMobsPrefix by StringSetting("Total Prefix", "§fMobs: ", desc = "Prefix for total mob counter")
    private val cowsPrefix by StringSetting("Cows Prefix", "§bCows: ", desc = "Prefix for cow counter")
    private val sheepPrefix by StringSetting("Sheep Prefix", "§eSheep: ", desc = "Prefix for sheep counter")
    private val chickensPrefix by StringSetting("Chicken Prefix", "§cChickens: ", desc = "Prefix for chicken counter")
    private val batsPrefix by StringSetting("Bats Prefix", "§1Bats: ", desc = "Prefix for bat counter")
    private val wolvesPrefix by StringSetting("Wolves Prefix", "§6Wolves: ", desc = "Prefix for wolf counter")
    private val rabbitsPrefix by StringSetting("Rabbits Prefix", "§2Rabbits: ", desc = "Prefix for rabbit counter")
    private val mobsUnderThornPrefix by StringSetting("Mobs Under Thorn Prefix", "§7Under Thorn: ", desc = "Prefix for mobs under thorn counter")

}