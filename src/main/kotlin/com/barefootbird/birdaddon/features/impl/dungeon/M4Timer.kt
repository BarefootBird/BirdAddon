package com.barefootbird.birdaddon.features.impl.dungeon

import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.features.Category

object M4Timer: Module(
    name = "M4Timer",
    description = "Shows how long the boss has gone on for",
    category = Category.BOSS
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