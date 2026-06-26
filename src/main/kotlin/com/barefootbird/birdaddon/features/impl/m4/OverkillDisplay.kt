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
        val compactPrefix: String,
        val amount: Int
    )

    private val hud by HUD(name, "Displays the overkill", false) { example ->

        fun renderCounters(counters: List<Counter>): Pair<Int, Int> {
            val visibleCounters = counters.filter { !hideZero || it.amount != 0 || it.prefix == "Overkill: " }
            val texts = visibleCounters.map { counter ->
                when {
                    compactMode -> "${counter.compactPrefix}: ${counter.amount}"
                    showPrefixes -> counter.prefix + counter.amount
                    else -> counter.amount.toString()
                }
            }

            if (compactMode) {
                var x = 0

                visibleCounters.zip(texts).forEach { (counter, text) ->
                    val (width, _) = textDim(text, x, 0, counter.color)
                    x += width + 4
                }

                return x.coerceAtLeast(0) to 9
            }

            var width = 0
            visibleCounters.zip(texts).forEachIndexed { index, (counter, text) ->
                val (lineWidth, _) = textDim(text, 0, index * 9, counter.color)
                width = maxOf(width, lineWidth)
            }

            return width to visibleCounters.size * 9
        }

        if (example) {
            val examples = listOf(
                Counter(Colors.WHITE, "Overkill: ", "O", 0),
                Counter(Highlight.cowColor, "Cows: ", "C", 0),
                Counter(Highlight.sheepColor, "Sheep: ", "S", 0),
                Counter(Highlight.chickenColor, "Chickens: ", "Ch", 0),
                Counter(Highlight.batColor, "Bats: ", "B", 0),
                Counter(Highlight.wolfColor, "Wolves: ", "W", 0),
                Counter(Highlight.rabbitColor, "Rabbits: ", "R", 0)
            )

            return@HUD renderCounters(examples)
        }

        if (!M4State.inBoss()) {
            return@HUD 0 to 0
        }

        val counters = listOf(
            Counter(Colors.WHITE, "Overkill: ", "O", overkill),
            Counter(Highlight.cowColor, "Cows: ", "C", overkillCows),
            Counter(Highlight.sheepColor, "Sheep: ", "S", overkillSheep),
            Counter(Highlight.chickenColor, "Chickens: ", "Ch", overkillChickens),
            Counter(Highlight.batColor, "Bats: ", "B", overkillBats),
            Counter(Highlight.wolfColor, "Wolves: ", "W", overkillWolves),
            Counter(Highlight.rabbitColor, "Rabbits: ", "R", overkillRabbits)
        )

        return@HUD renderCounters(counters)
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

    private val compactMode by BooleanSetting(
        "Compact Mode",
        false,
        desc = "Renders all overkill counters on one line with short mob names"
    )
}
