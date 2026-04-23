package com.barefootbird.birdaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.barefootbird.birdaddon.utils.M4State.overkill
import com.odtheking.odin.features.Category

object OverkillDisplay: Module(
    name = "Overkill Display",
    description = "Shows how much overkill in m4",
    category = Category.DUNGEON
) {
    private val hud by HUD(name, "Displays the overkill", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val showPrefix by BooleanSetting("Show Prefix", true, desc = "Shows 'Overkill: ' prefix")



    private fun timerText (example: Boolean): String {
        if (example) return "Overkill: 5"
        if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return ""
        if (showPrefix) {
            return "Overkill: $overkill"
        }
        return overkill.toString()
    }
}