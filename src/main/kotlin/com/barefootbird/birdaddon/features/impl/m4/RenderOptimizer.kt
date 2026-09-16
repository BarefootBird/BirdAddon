package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.Decoy.npcs
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState


object RenderOptimizer: Module(
    name = "M4 Render Optimizer",
    description = "Improves performance by not rendering certain things",
    category = Category.M4
) {
    private val hideParticles by BooleanSetting("Hide Particles", true, desc = "Hides all the particles")
    private val healerCircle by BooleanSetting("Except Healer Circle", true, desc = "Shows healer circle particles even when hide particles is on") // HAPPY_VILLAGER
    private val mageBeam by BooleanSetting("Except Mage Beam", true, desc = "Shows mage beam particles even when hide particles is on") // FIREWORK

    private val npcVisibility by SelectorSetting(
        "NPC Visibility",
        NpcVisibility.RELEVANT_ONLY,
        desc = "hides/shows npcs"
    )

    private val npcHighlight by SelectorSetting(
        "NPC Highlight",
        NpcHighlight.NONE,
        desc = "highlights npcs"
    )
    private val renderStyle by SelectorSetting(
        "Render Style",
        BoxStyle.OUTLINE,
        desc = "Style of the box."
    )

    private val hideInvisArmorStands by BooleanSetting("Hide Invis Armorstands", true, "Hides: Fairies, Bow Spirits, Grounded Chickens, Dialogue, Damage Splashes, and possibly more. Does not hide bow/tribal spear")

    private fun isRelevant(entity: Entity): Boolean {
        return entity.x > 17 && entity.z > 17
    }

    var ended = false

    @JvmStatic
    fun shouldHideParticle(particleOptions: ParticleOptions): Boolean {
        if (!enabled || !M4State.inBoss() || !hideParticles) return false
        val type = particleOptions.type
        if (type == ParticleTypes.HAPPY_VILLAGER && healerCircle) return false
        if (type == ParticleTypes.FIREWORK && mageBeam) return false
        return true
    }

    @JvmStatic
    fun shouldHideEntity(entity: Entity): Boolean {
        if (!M4State.inBoss() || !enabled) return false

        // Hide invis armorstands, but keep the ones that are holding items (bow and tribal spear)
        if (hideInvisArmorStands && entity is ArmorStand && entity.isInvisible && entity.mainHandItem.isEmpty && !ended) {
            return true
        }

        if (!npcs.contains(entity)) return false

        return npcVisibility == NpcVisibility.NONE ||
            (npcVisibility == NpcVisibility.RELEVANT_ONLY && !isRelevant(entity))
    }

    init {
        on<M4Event.End> {
            ended = true
        }

        on<LevelEvent.Load> {
            ended = false
        }

        on<RenderExtractEvent> {
            if (!M4State.inBoss()) return@on
            runCatching {
                val style = renderStyle

                if (npcHighlight != NpcHighlight.NONE) {
                    npcs.toList().forEach { entity ->
                        if (npcHighlight == NpcHighlight.RELEVANT_ONLY && isRelevant(entity)) {
                            drawStyledBox(entity.renderBoundingBox, Colors.MINECRAFT_GRAY, style, true)
                        } else if (npcHighlight == NpcHighlight.ALL) {
                            drawStyledBox(entity.renderBoundingBox, Colors.MINECRAFT_GRAY, style, true)
                        }
                    }
                }
            }
        }
    }
}
