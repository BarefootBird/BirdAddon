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
    private val printToChat by BooleanSetting("Print Tick Time", true, desc = "Prints run time in ticks to chat")
    private val printLastBowTime by BooleanSetting("Print Last Bow Time", true, desc = "Prints time taken to shoot last bow (Measured from when the bear dies to when boss ends)")

    private val hud by HUD(name, "Displays the time on the hud", true) { example ->
        when {
            example -> "§c48.2"
            !M4State.inBoss() -> null
            else -> "§c${(M4State.timer / 20f).toFixed()}s"
        }?.let { text ->
            textDim(text, 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }

    init {
        on<M4Event.End> {
            if (printToChat) {
                GlobalScope.launch {
                    delay(1000)
                    modMessage("Thorn Defeated in ${(M4State.timer / 20.0).toFixed(2)}s")
                }
            }
            if (printLastBowTime) {
                GlobalScope.launch {
                    delay(1000)
                    modMessage(
                        "Last Bow Shot In ${
                            ((M4State.timer - M4State.bearKillTimes[M4State.bearKillTimes.size - 1]) / 20.0).toFixed(
                                2
                            )
                        }s"
                    )
                }
            }
        }
    }
}