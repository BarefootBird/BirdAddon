package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.barefootbird.birdaddon.utils.M4State.overkill
import com.barefootbird.birdaddon.utils.M4State.overkillBats
import com.barefootbird.birdaddon.utils.M4State.overkillChickens
import com.barefootbird.birdaddon.utils.M4State.overkillCows
import com.barefootbird.birdaddon.utils.M4State.overkillRabbits
import com.barefootbird.birdaddon.utils.M4State.overkillSheep
import com.barefootbird.birdaddon.utils.M4State.overkillWolves

object OverkillDisplay: Module(
    name = "Overkill Display",
    description = "Shows how much overkill in m4",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays the overkill", false) { example ->
        textDim(text(example), 0, 0, Colors.WHITE)
    }
    private val showPrefix by BooleanSetting("Show Prefix", true, desc = "Shows 'Overkill: ' prefix")
    private val showMobType by BooleanSetting("Show Mob Type", true, desc = "Shows which mobs were overkilled")


    private fun pluralize(count: Int, mob: String): String {
        val plural = when (mob) {
            "bat" -> "bats"
            "chicken" -> "chickens"
            "sheep" -> "sheep"
            "cow" -> "cows"
            "rabbit" -> "rabbits"
            "wolf" -> "wolves"
            else -> mob + "s"
        }

        val name = if (count == 1) mob else plural
        return "$count $name"
    }

    private fun text (example: Boolean): String {
        if (example) return "Overkill: 5 (1 bat, 2 cows)"
        if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return ""

        var overkillString = "$overkill"

        if (showMobType) {
            val parts = listOfNotNull(
                overkillBats.takeIf { it > 0 }?.let { pluralize(it, "bat") },
                overkillChickens.takeIf { it > 0 }?.let { pluralize(it, "chicken") },
                overkillSheep.takeIf { it > 0 }?.let { pluralize(it, "sheep") },
                overkillCows.takeIf { it > 0 }?.let { pluralize(it, "cow") },
                overkillRabbits.takeIf { it > 0 }?.let { pluralize(it, "rabbit") },
                overkillWolves.takeIf { it > 0 }?.let { pluralize(it, "wolf") },
            )
            if (parts.isNotEmpty()) {
                overkillString += " (${parts.joinToString(", ")})"
            }
        }

        if (showPrefix) {
            return "Overkill: $overkillString"
        }
        return overkillString
    }
}