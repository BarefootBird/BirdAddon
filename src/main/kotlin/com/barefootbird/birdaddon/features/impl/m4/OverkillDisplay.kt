package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.barefootbird.birdaddon.utils.M4State.overkill
import com.barefootbird.birdaddon.utils.M4State.overkillBats
import com.barefootbird.birdaddon.utils.M4State.overkillChickens
import com.barefootbird.birdaddon.utils.M4State.overkillCows
import com.barefootbird.birdaddon.utils.M4State.overkillRabbits
import com.barefootbird.birdaddon.utils.M4State.overkillSheep
import com.barefootbird.birdaddon.utils.M4State.overkillWolves

object OverkillDisplay : Module(
    name = "Overkill Display",
    description = "Shows how much overkill in m4",
    category = Category.M4
) {

    private data class Counter(
        val color: com.odtheking.odin.utils.Color,
        val prefix: String,
        val amount: Int
    )

    private val hud by HUD(name, "Displays the overkill", false) { example ->

        fun renderCounters(counters: List<Counter>): Int {
            var y = 0

            counters
                .filter { !hideZero || it.amount != 0 || it.prefix == "Overkill: " }
                .forEach { counter ->

                    val text = if (showPrefixes) {
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
                Counter(Colors.WHITE, "Overkill: ", 0),
                Counter(Highlight.cowColor, "Cows: ", 0),
                Counter(Highlight.sheepColor, "Sheep: ", 0),
                Counter(Highlight.chickenColor, "Chickens: ", 0),
                Counter(Highlight.batColor, "Bats: ", 0),
                Counter(Highlight.wolfColor, "Wolves: ", 0),
                Counter(Highlight.rabbitColor, "Rabbits: ", 0)
            )

            val y = renderCounters(examples)
            return@HUD "Overkill: 00".length to y
        }

        if (!M4State.inBoss()) {
            return@HUD 0 to 0
        }

        val counters = listOf(
            Counter(Colors.WHITE, "Overkill: ", overkill),
            Counter(Highlight.cowColor, "Cows: ", overkillCows),
            Counter(Highlight.sheepColor, "Sheep: ", overkillSheep),
            Counter(Highlight.chickenColor, "Chickens: ", overkillChickens),
            Counter(Highlight.batColor, "Bats: ", overkillBats),
            Counter(Highlight.wolfColor, "Wolves: ", overkillWolves),
            Counter(Highlight.rabbitColor, "Rabbits: ", overkillRabbits)
        )

        val y = renderCounters(counters)

        return@HUD "Overkill: 00".length to y
    }

    private val hideZero by BooleanSetting(
        "Hide Zero",
        false,
        desc = "Hides counters that are 0"
    )

    private val showPrefixes by BooleanSetting(
        "Prefixes",
        true,
        desc = "Adds a prefix for each overkill type"
    )
}