package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.state.Timers
import com.barefootbird.birdaddon.utils.state.BossLifecycle
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(DelicateCoroutinesApi::class)
object Timer: Module(
    name = "Timer",
    key = null,
    description = "Shows how long the boss has gone on for",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays the time on the hud", true) { example ->
        when {
            example -> "§c48.2"
            !BossLifecycle.inBoss() -> null
            else -> "§c${(Timers.timer / 20f).toFixed()}s"
        }?.let { text ->
            textDim(text, 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }
}
