package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.features.impl.m4.Decoy.npcs
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState


object RenderOptimizer: Module(
    name = "Render Optimizer",
    description = "Improves performance by not rendering certain things",
    category = Category.M4
) {
    private val hideParticles by BooleanSetting("Hide Particles", true, desc = "Hides all the particles")
    private val healerCircle by BooleanSetting("Except Healer Circle", true, desc = "Shows healer circle particles even when hide particles is on") // HAPPY_VILLAGER
    private val mageBeam by BooleanSetting("Except Mage Beam", true, desc = "Shows mage beam particles even when hide particles is on") // FIREWORK

    private val hidePlants by BooleanSetting("Hide Plants", true, "Hides plants (You might need to reload your textures)")
    private val hideFire by BooleanSetting("Hide Fire Blocks", true, "Hides fire blocks (You might need to reload your textures)")

    private val npcVisibility by SelectorSetting("NPC Visibility", "Relevant only", listOf("All", "Relevant only", "None"), "hides/shows npcs")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val npcHighlight by SelectorSetting("NPC Highlight", "None", listOf("None", "Relevant only", "All"), "highlights npcs")

    private val hideInvisArmorStands by BooleanSetting("Hide Invis Armorstands", true, "Hides: Fairies, Bow Spirits, Grounded Chickens, Dialogue, Damage Splashes, and possibly more. Does not hide bow/tribal spear")

    private fun isRelevant(entity: Entity): Boolean {
        return entity.x > 17 && entity.z > 17
    }

    @JvmStatic
    fun shouldHideParticle(particleOptions: ParticleOptions): Boolean {
        if (!enabled || !M4State.inBoss() || !hideParticles) return false
        val type = particleOptions.type
        if (type == ParticleTypes.HAPPY_VILLAGER && healerCircle) return false
        if (type == ParticleTypes.FIREWORK && mageBeam) return false
        return true
    }

    @JvmStatic
    fun shouldHideBlock (blockState: BlockState): Boolean {
        if (!M4State.inBoss() || !enabled) return false
        if (hidePlants) {
            if (blockState.block == Blocks.JUNGLE_SAPLING) return true
            if (blockState.block == Blocks.SUNFLOWER) return true
        }
        if (hideFire && blockState.block == Blocks.FIRE) return true
        return false
    }

    @JvmStatic
    fun shouldHideEntity (entity: Entity): Boolean {
        if (!M4State.inBoss() || !enabled) return false
        // Hide invis armorstands, but keep the ones that are holding items (bow and tribal spear)
        if (hideInvisArmorStands && entity is ArmorStand && entity.isInvisible && entity.mainHandItem.isEmpty) return true
        if (!npcs.contains(entity)) return false
        return npcVisibility == 2 || (npcVisibility == 1 && !isRelevant(entity))
    }

    init {
        on<RenderEvent.Extract> {
            if (!M4State.inBoss()) return@on
            runCatching {
                val style = renderStyle

                if (npcHighlight != 0) {
                    npcs.toList().forEach { entity ->
                        if (npcHighlight == 1 && isRelevant(entity)) {
                            drawStyledBox(entity.renderBoundingBox, Colors.MINECRAFT_GRAY, style, true)
                        } else if (npcHighlight == 2) {
                            drawStyledBox(entity.renderBoundingBox, Colors.MINECRAFT_GRAY, style, true)
                        }
                    }
                }
            }
        }
    }
}