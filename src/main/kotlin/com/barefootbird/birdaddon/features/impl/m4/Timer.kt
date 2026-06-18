package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
object Timer: Module(
    name = "Timer",
    description = "Shows how long the boss has gone on for",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays the time on the hud", true) { example ->
        when {
            example -> "§c48.2"
            !M4State.inBoss() -> null
            else -> "§c${(M4State.timer / 20f).toFixed()}s"
        }?.let { text ->
            textDim(text, 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }
}