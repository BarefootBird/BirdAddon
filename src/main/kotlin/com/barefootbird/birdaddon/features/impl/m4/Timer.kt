package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting

object Timer: Module(
    name = "Timer",
    description = "Shows how long the boss has gone on for",
    category = Category.M4
) {
    val printToChat by BooleanSetting("Print To Chat", true, desc = "Prints run time to chat")

    private val hud by HUD(name, "Displays the time on the hud", true) { example ->
        when {
            example -> "§c48.2"
            !DungeonUtils.isFloor(4) || !DungeonUtils.inBoss -> null
            else -> "§c${(M4State.timer / 20f).toFixed()}s"
        }?.let { text ->
            textDim(text, 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }
}