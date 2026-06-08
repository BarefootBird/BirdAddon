package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4Mobs
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.drawStyledBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object Trajectories : Module (
        name = "Trajectories",
        description = "Shows trajectories of sheeps/cows/chickens",
        category = Category.M4
) {
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val cows by BooleanSetting("Cows", true, desc = "Highlight where the cows will land")
    private val sheep by BooleanSetting("Sheep", true, desc = "Highlight where the sheep will land")
    private val chicken by BooleanSetting("Chicken", true, "Highlight where the chicken will land")

    private val cowLandingSpots = mutableSetOf<Vec3>()
    private val sheepLandingSpots = mutableSetOf<Vec3>()

    fun getGroundY(x: Double, z: Double): Double {
        val level = mc.level ?: return 69.0

        val blockX = x.toInt()
        val blockZ = z.toInt()

        for (y in 100 downTo 69) {
            val pos = BlockPos(blockX, y, blockZ)

            val block = level.getBlockState(pos)

            if (!block.isAir && block.isSolid) {
                return y + 1.0
            }
        }

        return 69.0
    }


    // Given velocity and position, predicts the landing spot for cows/sheep
    // Assumes that they'll always land at y = 69, which is mostly true besides when you get the beef on leaf
    fun predictLanding(
        startPos: Vec3,
        startVel: Vec3
    ): Vec3? {

        var x = startPos.x
        var y = startPos.y
        var z = startPos.z

        var vx = startVel.x
        var vy = startVel.y
        var vz = startVel.z

        repeat(100) {

            // Apply Minecraft physics
            x += vx
            y += vy
            z += vz

            vx *= 0.98
            vz *= 0.98
            vy = (vy - 0.08) * 0.98

            val groundY = 69.0

            if (y <= groundY) {
                return Vec3(x, groundY, z)
            }
        }
        return null
    }

    init {
        on<TickEvent.Server> {
            cowLandingSpots.clear()
            sheepLandingSpots.clear()
            if (cows) {
                runCatching {
                    M4Mobs.cows.forEach {
                        if (it.y > 71.5) {
                            val landingSpot = predictLanding(it.position(), it.deltaMovement)
                            if (landingSpot != null) {
                                cowLandingSpots.add(landingSpot)
                            }
                        }
                    }
                }
            }
            if (sheep) {
                runCatching {
                    M4Mobs.sheep.forEach {
                        if (it.y > 71.5) {
                            val landingSpot = predictLanding(it.position(), it.deltaMovement)
                            if (landingSpot != null) {
                                sheepLandingSpots.add(landingSpot)
                            }
                        }
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            runCatching {
                cowLandingSpots.forEach {
                    val box = AABB(it.x - 0.2, it.y, it.z - 0.2, it.x + 0.2, it.y, it.z + 0.2)
                    drawStyledBox(box, Highlight.cowColor,renderStyle, false)
                }
                sheepLandingSpots.forEach {
                    val box = AABB(it.x - 0.2, it.y, it.z - 0.2, it.x + 0.2, it.y, it.z + 0.2)
                    drawStyledBox(box, Highlight.sheepColor,renderStyle, false)
                }
                M4Mobs.chickens.forEach{
                    val y = getGroundY(it.x, it.z)
                    val box = AABB(it.x - 0.2, y, it.z - 0.2, it.x + 0.2, y, it.z + 0.2)
                    drawStyledBox(box, Highlight.chickenColor,renderStyle, false)
                }
            }
        }
    }
}