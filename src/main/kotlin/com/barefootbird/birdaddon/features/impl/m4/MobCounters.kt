package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim

object MobCounters : Module(
    name = "Mob Counter",
    description = "Shows how many mobs are alive in m4",
    category = Category.M4
) {

    private data class Counter(
        val enabled: Boolean,
        val color: com.odtheking.odin.utils.Color,
        val prefix: String,
        val amount: Int
    )

    private val hud by HUD(name, "Mob Counter Hud", false) { example ->

        fun renderCounters(counters: List<Counter>): Int {
            var y = 0

            counters
                .filter { it.enabled }
                .forEach { counter ->
                    val text = if (prefixes) {
                        counter.prefix + counter.amount
                    } else {
                        counter.amount.toString()
                    }

                    textDim(text, 0, y, counter.color)
                    y += 9
                }

            return y
        }

        if (example) {
            val examples = listOf(
                Counter(totalGUI, Colors.WHITE, "Mobs: ", 0),
                Counter(cowsGUI, Highlight.cowColor, "Cows: ", 0),
                Counter(sheepGUI, Highlight.sheepColor, "Sheep: ", 0),
                Counter(chickensGUI, Highlight.chickenColor, "Chickens: ", 0),
                Counter(batsGUI, Highlight.batColor, "Bats: ", 0),
                Counter(wolvesGUI, Highlight.wolfColor, "Wolves: ", 0),
                Counter(rabbitsGUI, Highlight.rabbitColor, "Rabbits: ", 0),
                Counter(mobsUnderThornGui, Highlight.thornColor, "Under Thorn: ", 0),
                Counter(rabbitsNotUnderThornGui, Highlight.rabbitColor, "Rabbits in Mid: ", 0)
            )

            var y = renderCounters(examples)

            return@HUD "Rabbits Under Thorn: 00".length to y
        }

        if (!M4State.inBoss()) {
            return@HUD 0 to 0
        }

        val allMobs = listOf(
            M4Mobs.cows,
            M4Mobs.sheep,
            M4Mobs.chickens,
            M4Mobs.bats,
            M4Mobs.wolves,
            M4Mobs.rabbits
        )

        val mobsUnderThorn = allMobs.sumOf { mobList ->
            mobList.count { isUnderThorn(it.x, it.y, it.z) }
        }

        val rabbitsInMid = M4Mobs.rabbits.count {
            !isUnderThorn(it.x, it.y, it.z)
        }

        val counters = listOf(
            Counter(totalGUI, Colors.WHITE, "Mobs: ", allMobs.sumOf { it.size }),
            Counter(cowsGUI, Highlight.cowColor, "Cows: ", M4Mobs.cows.size),
            Counter(sheepGUI, Highlight.sheepColor, "Sheep: ", M4Mobs.sheep.size),
            Counter(chickensGUI, Highlight.chickenColor, "Chickens: ", M4Mobs.chickens.size),
            Counter(batsGUI, Highlight.batColor, "Bats: ", M4Mobs.bats.size),
            Counter(wolvesGUI, Highlight.wolfColor, "Wolves: ", M4Mobs.wolves.size),
            Counter(rabbitsGUI, Highlight.rabbitColor, "Rabbits: ", M4Mobs.rabbits.size),
            Counter(mobsUnderThornGui, Highlight.thornColor, "Under Thorn: ", mobsUnderThorn),
            Counter(rabbitsNotUnderThornGui, Highlight.rabbitColor, "Rabbits in Mid: ", rabbitsInMid)
        )

        val y = renderCounters(counters)
        return@HUD "Rabbits Under Thorn: 00".length to y
    }

    private fun isUnderThorn(x: Double, y: Double, z: Double): Boolean {
        return y < 75 && ((x > 21 && z > 21) || (x > 26 && z > 16) || (x > 16 && z > 26))
    }

    private val totalGUI by BooleanSetting("Total Mobs", true, desc = "Show Total Mobs")
    private val cowsGUI by BooleanSetting("Cows", true, desc = "Shows alive cows")
    private val sheepGUI by BooleanSetting("Sheep", true, desc = "Shows alive sheep")
    private val chickensGUI by BooleanSetting("Chickens", true, desc = "Shows alive chickens")
    private val batsGUI by BooleanSetting("Bats", true, desc = "Shows alive bats")
    private val wolvesGUI by BooleanSetting("Wolves", true, desc = "Shows alive wolves")
    private val rabbitsGUI by BooleanSetting("Rabbits", true, desc = "Shows alive rabbits")
    private val mobsUnderThornGui by BooleanSetting(
        "Mobs Under Thorn",
        true,
        desc = "Shows mobs under thorn"
    )
    private val rabbitsNotUnderThornGui by BooleanSetting(
        "Rabbits Not Under Thorn",
        true,
        desc = "Shows the number of rabbits that aren't under thorn"
    )

    private val prefixes by BooleanSetting(
        "Prefixes",
        true,
        desc = "Adds a prefix for each of the mob types"
    )
}