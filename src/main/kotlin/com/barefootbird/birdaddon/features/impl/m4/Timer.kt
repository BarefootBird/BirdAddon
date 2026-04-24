package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.M4State

object Timer: Module(
    name = "Timer",
    description = "Shows how long the boss has gone on for",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", false) { example ->
        when {
            example -> "§c48.2"
            !DungeonUtils.isFloor(4) || !DungeonUtils.inBoss -> null
            else -> "§c${(M4State.timer / 20f).toFixed()}s"
        }?.let { text ->
            textDim(text, 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }
}