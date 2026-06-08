package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.Vec2
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.skeleton.Skeleton
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.entity.monster.zombie.Zombie


import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


object Decoy: Module(
    name = "Decoy",
    description = "Thing for m4 decoys",
    category = Category.M4
) {
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val highlightBestDecoySpot by BooleanSetting("Show best decoy spot", true, desc = "Highlights the best available decoy spot")
    // x + z coords of the spots in order of best to worst
    private val bestSpots = listOf(
        Vec2(26, 29), Vec2(29, 26), Vec2(31, 28), Vec2(27, 32), // front 4
        Vec2(21, 31), Vec2(32, 21), Vec2(19, 33), Vec2(33, 19), // side 4
        Vec2(30, 32), Vec2(33, 30), Vec2(27, 35), // mid left 3
        Vec2(32, 34), Vec2(35, 31), Vec2(29, 36), // back left 3
        Vec2(34, 24), Vec2(35, 27), Vec2(37, 27), // right 3
        Vec2(27, 38), Vec2(24, 37), Vec2(21, 36), Vec2(24, 40), // some extra left ones just in case
    )

    private var bestSpotIndex = 100
    private var bestSpot: Vec3? = null

    private val searchBox = AABB(-36.0, 77.0, -36.0, 47.0, 83.0, 47.0)

    val npcs: MutableSet<Entity> = mutableSetOf()

    private fun addNpc(entity: Entity) {
        npcs.add(entity)

        if (highlightBestDecoySpot) {
            bestSpots.forEachIndexed { index, spot ->
                if (index >= bestSpotIndex) {
                    return
                }
                if (spot.x + 0.5 == entity.x && spot.z + 0.5 == entity.z) {
                    bestSpotIndex = index
                    bestSpot = entity.position()
                }
            }
        }
    }

    init {

        on<TickEvent.Server> {
            if (!M4State.inBoss()) return@on

            runCatching {
                val allEntities = mc.level?.getEntities(null, searchBox)?.toList() ?: emptyList()
                allEntities.forEach { entity ->
                    if (entity is Zombie) addNpc(entity)
                    if (entity is Skeleton) addNpc(entity)
                    if (entity is WitherSkeleton) addNpc(entity)
                    if (entity is Player) {
                        if (entity.gameProfile.name == "Lost Adventurer") addNpc(entity)
                        if (entity.gameProfile.name == "Crypt Souleater") addNpc(entity)
                        if (entity.gameProfile.name == "Crypt Dreadlord") addNpc(entity)
                        if (entity.gameProfile.name == "Diamond Guy") addNpc(entity) // Angry Archaeologist
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            if (!M4State.inBoss()) return@on
            if (bestSpot != null) {
                val box = AABB(bestSpot!!.x - 0.5, bestSpot!!.y, bestSpot!!.z - 0.5, bestSpot!!.x + 0.5, bestSpot!!.y, bestSpot!!.z + 0.5)
                drawFilledBox(box, Colors.MINECRAFT_RED, true)
            }
        }

        on<WorldEvent.Load> {
            bestSpotIndex = 100
            bestSpot = null // placeholder value
            npcs.clear()
        }
    }
}