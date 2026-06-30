package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

object ThornStunTimer: Module(
    name = "Stun",
    description = "Shows information relevant to the stun",
    category = Category.M4
) {
    private val hud by HUD(name, "Displays how long thorn is stunned for", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }

    private val onlyShowOnHealer by BooleanSetting("Only show on healer", true, "Only shows stun related features when you're on healer")

    private val decimals by NumberSetting("Decimals", 2, 1, 2, 1, "How many decimals to show")

    private val stunnedText by StringSetting(
        "Stunned Text",
        $$"§5Stunned for $timer",
        desc = "HUD format for when thorn is stunned"
    )

    private val notStunnedText by StringSetting(
        "Not Stunned Text",
        "§5Stunned for §cN/A",
        desc = "HUD format for when thorn isn't stunned"
    )

    private val stunHelper by BooleanSetting("Stun Helper", true, "Shows when to stun")

    private fun parseTemplate(template: String): String {
        return template.replace($$"$timer", "${(timer / 20.0).toFixed(decimals)}s")
    }
    private fun timerText (example: Boolean): String {
        if (example) return parseTemplate(notStunnedText)
        if (!M4State.inBoss()) return ""
        if (onlyShowOnHealer && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.HEALER) return ""

        if (timer >= 0) {
            return parseTemplate(stunnedText)
        }
        return parseTemplate(notStunnedText)
    }
    private var timer = 310
    private var firstTimer = 310
    private var secondTimer = -1
    private var secondTimerStarted = false

    private val firstStunPos = Vec3(1.5, 86.0, 28.5)
    private val secondStunPos = Vec3(25.5, 85.0, 20.5)

    private val secondTimerVariable by NumberSetting("second stun offset", 5, 0, 10, 1, "lower values make stun further away, higher values make it closer")

    init {
        onReceive<ClientboundHurtAnimationPacket> {
            if (!M4State.inBoss()) return@onReceive
            val entity = OdinMod.mc.level!!.getEntity(this.id)
            if (entity is Ghast) {
                timer = 82
            }
        }

        on<RenderEvent.Extract> {
            if (!stunHelper || !M4State.inBoss()) return@on
            if (onlyShowOnHealer && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.HEALER) return@on

            if (firstTimer != -1) drawText("§5${(firstTimer / 20.0).toFixed(decimals)}s", firstStunPos, 2f, false)
            if (secondTimer != -1) drawText("§5${(secondTimer / 20.0).toFixed(decimals)}s", secondStunPos, 2f, false)
        }

        on<TickEvent.Server> {
            if (!M4State.inBoss()) return@on
            if (timer > -1) {
                timer--
            }
            if (firstTimer > -1) {
                firstTimer--
            }
            if (secondTimer > -1) {
                secondTimer--
            } else {
                val thorn = M4Mobs.thorn ?: return@on
                val angle = atan2(thorn.x - 5.5, thorn.z - 5.5)
                /*
                thorn starts with angle of 0, moves towards negative angles before first stun.
                the timer is based on when thorn moves past 0 in to the positive direction
                */
                if (angle > 0 && !secondTimerStarted) {
                    secondTimer = 28 + secondTimerVariable
                    secondTimerStarted = true
                }
            }
        }

        on<LevelEvent.Load> {
            timer = 310
            firstTimer = 300
            secondTimer = -1
            secondTimerStarted = false
        }
    }
}