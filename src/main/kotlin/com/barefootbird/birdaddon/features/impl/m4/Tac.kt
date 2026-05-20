package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.M4State.bearKillRegex
import com.barefootbird.birdaddon.utils.M4State.bearKillTimes
import com.barefootbird.birdaddon.utils.M4State.bearSpawnRegex
import com.barefootbird.birdaddon.utils.M4State.bearSpawnStartTimes
import com.barefootbird.birdaddon.utils.M4State.bearSpawnTimes
import com.barefootbird.birdaddon.utils.M4State.bearTimer
import com.barefootbird.birdaddon.utils.M4State.endRegex
import com.barefootbird.birdaddon.utils.M4State.ended
import com.barefootbird.birdaddon.utils.M4State.enteredRegex
import com.barefootbird.birdaddon.utils.M4State.inThornBoss
import com.barefootbird.birdaddon.utils.M4State.timer
import com.barefootbird.birdaddon.utils.M4State.updateKills
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand


object Tac: Module(
    name = "Tac",
    description = "Stuff for tactical insertion",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays how long thorn is stunned for", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val tacTimer by BooleanSetting("Tac Timer", true, "Timer for tac")
    private val outsideOfM4 by BooleanSetting("Show Tac Timer out of m4", false, "Shows tac timer outside of m4").withDependency { tacTimer }
    private val prefix by StringSetting("Tac Timer Prefix", "§6Tac: ", desc="Prefix for the tac timer").withDependency { tacTimer }

    val printTacTime by BooleanSetting("Print Last Bear Tac Time", true, "Print tac time")
    private val printTimeOnlyOnTank by BooleanSetting("Print Time Only On Tank", true, "Prints the last bear tac time only when on tank")

    private fun timerText (example: Boolean): String {
        if (example) return "${prefix}1.55s"
        if ((!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) && !outsideOfM4) return ""
        if (tacTime >= 0) {
            return "$prefix${(tacTime / 20.0).toFixed(2)}s"
        }
        return ""
    }

    var tacTime = -1
    var lastBearTacTime = -1
    var lastBearTaccedOn = -1

    init {

        onSend<ServerboundUseItemPacket> { event ->
            val item = mc.player?.getItemInHand(InteractionHand.MAIN_HAND)
            if (item?.displayName?.string?.lowercase()?.contains("tactical insertion") == true) {
                tacTime = 60
                if (printTacTime && bearTimer != -1) {
                    if (DungeonUtils.inBoss && DungeonUtils.isFloor(4)) {
                        if (DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.Tank || !printTimeOnlyOnTank) {
                            lastBearTacTime = bearTimer
                            lastBearTaccedOn = bearSpawnStartTimes.size + 1
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

        on<WorldEvent.Load> {
            lastBearTacTime = -1
            lastBearTaccedOn = -1
        }
    }
}