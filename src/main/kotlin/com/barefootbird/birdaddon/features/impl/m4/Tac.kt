package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.barefootbird.birdaddon.utils.sendCommand
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand


@OptIn(DelicateCoroutinesApi::class)
object Tac: Module(
    name = "Tac",
    description = "Stuff for tactical insertion",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays how much time is left on the Tactical Insertion ability", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val tacTimer by BooleanSetting("Tac Timer", true, "Timer for tac")
    private val outsideOfM4 by BooleanSetting("Show Tac Timer out of m4", false, "Shows tac timer outside of m4").withDependency { tacTimer }
    private val prefix by StringSetting("Tac Timer Prefix", "§6Tac: ", desc="Prefix for the tac timer").withDependency { tacTimer }

    private val printTacTime by BooleanSetting("Print Tac Time", true, "Print tac time")
    private val printTimeOnlyOnTank by BooleanSetting("Print Time Only On Tank", true, "Prints the tac time only when on tank")
    private val printTacToPartyChat by BooleanSetting("Print Tac to pchat", true, "Prints tac time to party chat")

    private fun timerText (example: Boolean): String {
        if (example) return "${prefix}1.55s"
        if (!M4State.inBoss() && !outsideOfM4) return ""
        if (tacTime >= 0) {
            return "$prefix${(tacTime / 20.0).toFixed(2)}s"
        }
        return ""
    }

    var tacTime = -1

    init {

        onSend<ServerboundUseItemPacket> { event ->
            val item = mc.player?.getItemInHand(InteractionHand.MAIN_HAND)
            if (item?.displayName?.string?.lowercase()?.contains("tactical insertion") == true) {
                if (tacTimer && tacTime == -1) {
                    tacTime = 60
                }
                if (printTacTime && M4State.bearTimer != -1) {
                    if (DungeonUtils.inBoss && DungeonUtils.isFloor(4)) {
                        if (DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.Tank || !printTimeOnlyOnTank) {
                            if (printTacToPartyChat) {
                                sendCommand("pc BirdAddon > Tacced at ${((M4State.bearTimer / 20.0).toFixed(2))}s")
                            } else {
                                modMessage(
                                    "Tacced at ${((M4State.bearTimer / 20.0).toFixed(2))}s"
                                )
                            }
                        }
                    }
                }
            }
        }

        on<TickEvent.Server> {
            if (tacTime >= 0) {
                tacTime--
            }
        }
    }
}