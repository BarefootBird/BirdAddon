package com.barefootbird.birdaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.features.Category
import com.odtheking.odin.utils.render.drawStyledBox
import net.minecraft.world.phys.AABB


object M4Waypoints: Module(
    name = "M4 Waypoints",
    description = "Waypoints for m4",
    category = Category.BOSS
) {
    private val swapRabbits by BooleanSetting("Swap Rabbit Waypoints", true, "Swaps the tank and bers waypoints for rabbits")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    private fun box1x1 (x: Int, y: Int, z: Int): AABB {
        return AABB(x + 0.0, y + 0.0, z + 0.0, x+1.0, y+1.0, z+1.0)
    }

    private fun RenderEvent.Extract.renderWaypoint (x: Int, y: Int, z: Int) {
        val color = when (DungeonUtils.currentDungeonPlayer.clazz) {
            DungeonClass.Healer -> Colors.MINECRAFT_LIGHT_PURPLE
            DungeonClass.Tank -> Colors.MINECRAFT_GREEN
            DungeonClass.Berserk -> Colors.MINECRAFT_DARK_RED
            DungeonClass.Archer -> Colors.MINECRAFT_GOLD
            DungeonClass.Mage -> Colors.MINECRAFT_BLUE
            else -> return
        }
        drawStyledBox(box1x1(x, y, z), color, renderStyle, false)
    }

    private fun RenderEvent.Extract.renderTank () {
        renderWaypoint(27, 81, 18) // leaf
        renderWaypoint(5, 68, 3) // bow spawn

        if (M4State.bearSpawnTimes.isEmpty()) {
            // wolf spawns
            drawStyledBox(AABB(27.5, 68.0, -17.5, 28.5, 69.0, -18.5), Colors.MINECRAFT_GREEN, renderStyle, false)
            drawStyledBox(AABB(35.5, 68.0, 4.5, 36.5, 69.0, 5.5), Colors.MINECRAFT_GREEN, renderStyle, false)
        }
        if (M4State.bearSpawnTimes.size == 1 && M4State.bearSpawnStartTimes.size == 1) {
            if (swapRabbits) {
                renderWaypoint(-10, 68, -11)
            } else {
                renderWaypoint(21, 68, -11)
            }
        }
    }

    private fun RenderEvent.Extract.renderHealer () {
        if (M4State.timer / 20 < 15) {
            renderWaypoint(-5, 83, 27) // leaf 1
        }
        if (M4State.timer / 20 in 15..<22) {
            renderWaypoint(27, 81, 18) // leaf 2
        }
        if (M4State.timer / 20 > 21) {
            renderWaypoint(29, 78, 29) // next to decoys
        }
    }

    private fun RenderEvent.Extract.renderBerserk () {
        if (M4State.bearSpawnTimes.isEmpty()) {
            renderWaypoint(26, 77, 26) // front edge
            renderWaypoint(28, 80, 19) // leaf camp
        }
        if (M4State.bearSpawnTimes.size == 1 && M4State.bearSpawnStartTimes.size == 1) {
            renderWaypoint(26, 77, 26) // front edge
            if (swapRabbits) {
                renderWaypoint(21, 68, -11)
            } else {
                renderWaypoint(-10, 68, -11)
            }
        }
        if (M4State.bearSpawnTimes.size == 2 && M4State.bearSpawnStartTimes.size == 3) {
            renderWaypoint(26, 77, 26)
        }
        if (M4State.bearSpawnTimes.size == 3 && M4State.bearSpawnStartTimes.size == 4) {
            renderWaypoint(25, 68, 25)
        }
        if (M4State.bearKillTimes.size >= 4) {
            renderWaypoint(28, 76, 27)
            renderWaypoint(28, 80, 19)
        }
    }

    private fun RenderEvent.Extract.renderArcher () {
        if (M4State.timer / 20 in 17..<20) {
            renderWaypoint(5, 68, 5)
        }
        if (M4State.bearKillTimes.size == 1 && M4State.bearSpawnStartTimes.size == 1) {
            renderWaypoint(-11, 68, 21)
        }
        if (M4State.bearKillTimes.size == 2 && M4State.bearSpawnStartTimes.size == 3) {
            renderWaypoint(13, 68, 13)
        }
    }

    private fun lookingAt (box: AABB): Boolean {

        val eyePos = mc.player?.eyePosition ?: return false
        val lookVec = mc.player?.getViewVector(1.0F) ?: return false
        val end = eyePos.add(lookVec.scale(100.0))
        return box.clip(eyePos, end).isPresent
    }

    private fun RenderEvent.Extract.renderMage () {
        val bearSpawn = AABB(5.75, 70.4, 5.75, 6.25, 71.5, 6.25) // only 1 block tall because aiming at the feet or head is a bit silly
        val bearSpawnColor: Color = if (M4State.bearTimer == -1) {
            Colors.MINECRAFT_BLUE
        } else {
            if (lookingAt(bearSpawn)) {
                Colors.MINECRAFT_GREEN
            } else {
                Colors.MINECRAFT_RED
            }
        }

        drawStyledBox(bearSpawn, bearSpawnColor, renderStyle, false) // bear spawn

        if (M4State.bearSpawnTimes.size == 1 && M4State.bearSpawnStartTimes.size == 1) {
            renderWaypoint(21, 68, 21) // rabbits
        }
        if (M4State.timer / 20 in 18..<20) {
            renderWaypoint(5, 68, 5) // gyro spot
        }

        // bear kill spots
        if (M4State.bearSpawnTimes.isEmpty()) {
            renderWaypoint(21, 68, 21)
        }
        if (M4State.bearSpawnTimes.size == 1) {
            renderWaypoint(11, 68, 6)
        }
        if (M4State.bearSpawnTimes.size == 2) {
            renderWaypoint(10, 68, 10)
            renderWaypoint(6, 68, 6)
        }
        if (M4State.bearSpawnTimes.size == 3) {
            renderWaypoint(13, 68, 13)
        }

        // gyro
        if (M4State.bearSpawnTimes.size == 4) {
            renderWaypoint(13, 68, 13)
        }
    }

    init {
        on<RenderEvent.Extract> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            when (DungeonUtils.currentDungeonPlayer.clazz) {
                DungeonClass.Healer -> renderHealer()
                DungeonClass.Tank -> renderTank()
                DungeonClass.Berserk -> renderBerserk()
                DungeonClass.Archer -> renderArcher()
                DungeonClass.Mage -> renderMage()
                else -> return@on
            }
        }
    }
}