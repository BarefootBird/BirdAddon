package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.state.M4Mobs
import com.barefootbird.birdaddon.utils.state.KillTracker
import com.barefootbird.birdaddon.utils.state.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim

object OverkillDisplay : Module(
    name = "Overkill Display",
    key = null,
    description = "Shows how much overkill in m4",
    category = Category.M4
) {

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

    private data class Counter(
        val prefix: String,
        val compactPrefix: String,
        val color: com.odtheking.odin.utils.Color,
        val amount: Int
    )

    private val hud by HUD(name, "Displays the overkill", false) {
        if (!M4State.inBoss()) return@HUD 0 to 0


        val counts = KillTracker.overkill

        val types = M4Mobs.animalTypes

        val mobCounters = types.map {
            val name = M4Mobs.mobNameFor(it)
            val short = M4Mobs.mobNameFor(it, short=true)
            Counter(
                prefix = name,
                compactPrefix = short,
                color = M4Mobs.mobColorFor(it),
                amount = counts.getValue(it)
            )
        }

        val counters = listOf(
            Counter("Overkill", "O", Colors.WHITE, counts.values.sum())
        ) + mobCounters


        val visibleCounters = counters.filter {
            !hideZero || it.amount != 0 || it.prefix == "Overkill"
        }

        val texts = visibleCounters.map { counter ->
            when {
                compactMode -> "${counter.compactPrefix}: ${counter.amount}"
                showPrefixes -> "${counter.prefix}: ${counter.amount}"
                else -> counter.amount.toString()
            }
        }

        if (compactMode) {
            var x = 0
            visibleCounters.zip(texts).forEach { (counter, text) ->
                val (width, _) = textDim(text, x, 0, counter.color)
                x += width + 4
            }
            x.coerceAtLeast(0) to 9
        } else {
            var width = 0
            visibleCounters.zip(texts).forEachIndexed { index, (counter, text) ->
                val (lineWidth, _) = textDim(text, 0, index * 9, counter.color)
                width = maxOf(width, lineWidth)
            }
            width to visibleCounters.size * 9
        }
    }
}