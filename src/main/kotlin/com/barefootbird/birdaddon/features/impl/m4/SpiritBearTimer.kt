package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.StringSetting

object SpiritBearTimer: Module(
    name = "Spirit Bear Timer",
    description = "Shows the state of the spirit bear spawns",
    category = Category.M4
) {
    private val killPhaseText by StringSetting(
        "Kill Phase Text",
        $$"§6Bear $bear: §a$kills/$cap",
        desc = $$"HUD format for when bears are being killed"
    )

    private val spawningText by StringSetting(
        "Spawning Text",
        $$"§6Bear $bear: §c$timer",
        desc = $$"HUD format for when bear is about to spawn"
    )

    private val spawnedText by StringSetting(
        "Spawned Text",
        $$"§6Bear $bear: §4!",
        desc = $$"HUD format for when bear has spawned"
    )

    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val showTicks by BooleanSetting("Show Timer in Ticks", true, desc = "Changes the timer to be in ticks instead of seconds")


    private fun timerText(example: Boolean): String {

        if (example) return parseTemplate(killPhaseText, 1, "§c1.55s", 0, 30)

        if (!M4State.inBoss()) return ""

        val text = when {
            M4State.bearTimer > 0 -> spawningText
            M4State.bearTimer == 0 -> spawnedText
            else -> killPhaseText
        }

        val bear = M4State.bearKillTimes.size + 1
        val kills = M4State.kills
        val cap = M4State.maxKills

        val timerValue = when {
            M4State.bearTimer > 0 -> {
                val ticksLeft = M4State.bearTimer.toDouble()
                if (showTicks) {
                    "§c${ticksLeft.toFixed(0).padStart(2, '0')}"
                } else {
                    "§c${(ticksLeft / 20.0).toFixed()}s"
                }
            }
            else -> "n/a"
        }

        return parseTemplate(text, bear, timerValue, kills, cap)
    }

    private fun parseTemplate(template: String, bear: Int, timer: String, kills: Int, cap: Int): String {
        return template
            .replace($$"$bear", bear.toString())
            .replace($$"$timer", timer)
            .replace($$"$kills", kills.toString().padStart(2, '0'))
            .replace($$"$cap", cap.toString())
    }
}