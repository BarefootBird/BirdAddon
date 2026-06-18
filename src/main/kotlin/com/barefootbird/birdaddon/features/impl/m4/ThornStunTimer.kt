package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.world.entity.monster.Ghast

object ThornStunTimer: Module(
    name = "Thorn Stun Timer",
    description = "Shows how long thorn is stunned for",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays how long thorn is stunned for", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    private val onlyShowOnHealer by BooleanSetting("Only show on healer", true, "Only shows the hud when you're on healer")

    private val decimals by NumberSetting("Decimals", 2, 1, 2, 1, "How many decimals to show")

    private fun timerText (example: Boolean): String {
        if (example) return "§cNot Stunned"
        if (!M4State.inBoss()) return ""
        if (onlyShowOnHealer && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.Healer) return ""
        if (timer >= 0) {
            return "§5${(timer / 20.0).toFixed(decimals)}s"
        }
        return "§cNot Stunned"
    }
    private var timer = 310

    init {
        onReceive<ClientboundHurtAnimationPacket> {
            if (!M4State.inBoss()) return@onReceive
            val entity = OdinMod.mc.level!!.getEntity(this.id)
            if (entity is Ghast) {
                timer = 82
            }
        }

        on<TickEvent.Server> {
            if (!M4State.inBoss()) return@on
            if (timer > -1) {
                timer--
            }
        }

        on<WorldEvent.Load> {
            timer = 310
        }
    }
}