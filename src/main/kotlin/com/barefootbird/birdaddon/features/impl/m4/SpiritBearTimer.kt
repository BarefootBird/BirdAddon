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
    private val prefix by StringSetting("Prefix", "§6Spirit Bear: ", desc = "Prefix for the hud")

    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val showTicks by BooleanSetting("Show Timer in Ticks", true, desc = "Changes the timer to be in ticks instead of seconds")


    private fun timerText (example: Boolean): String {
        if (example) return "${prefix}§c48"
        if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return ""
        if (M4State.bearTimer < 0) return  "${prefix}§a${M4State.kills.toString().padStart(2, '0')}"
        if (M4State.bearTimer > 0) {
            val ticksLeft: Double = M4State.bearTimer.toDouble()

            if (showTicks) return "${prefix}§c${(ticksLeft.toFixed(0).padStart(2, '0'))}"
            return "${prefix}§c${(ticksLeft / 20.0).toFixed()}s"
        }
        return "${prefix}§c!"
    }
}