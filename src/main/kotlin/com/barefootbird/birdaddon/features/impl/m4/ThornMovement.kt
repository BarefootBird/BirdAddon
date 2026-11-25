package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.debugMessage
import com.barefootbird.birdaddon.utils.modMessage
import com.barefootbird.birdaddon.utils.state.Mobs
import com.barefootbird.birdaddon.utils.state.BossLifecycle
import com.barefootbird.birdaddon.utils.state.Timers
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.renderPos
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.ranges.rangeTo

object ThornMovement: Module(
    name = "Thorn Movement",
    key = null,
    description = "Shows information about thorns movement",
    category = Category.Testing
) {

    private val renderPath by BooleanSetting("Render Path", true, "Renders thorn's path")
    private val renderSpeed by BooleanSetting("render Speed", true, "Renders thorns speed")
    private val colorPathBySpeed by BooleanSetting("Color Path By Speed", true, "Red = 0.35, blue = 0.27")

    private val directionLine by BooleanSetting("direction line", true, "Renders a line with the direction thorn is facing")
    private val bidirectionalLine by BooleanSetting("Biderectional Line", true, "Renders the line in both the direction thorn is going and where they came from")
    private val constantY by BooleanSetting("flat direction line", true, "Makes the line have a constant y value instead of going up/down")

    private val printSpeed by BooleanSetting("printSpeed", false, "")
    private val printPosAtCorner by BooleanSetting("printPosAtCorner", false, "")
    private val printPosAndDirectionAtCorner by BooleanSetting("printPosAndDirAtCorner", false, "print position and direction of thorn at the corners")
    private val printLineEquationAtCorner by BooleanSetting("PrintLineEquationAtCorner", false, "prints the equation for the line that thorn is travelling on at the corners")

    private val thornSim by BooleanSetting("Thorn Sim", true, "")
    private val renderSimPath by BooleanSetting("Render Sim Path", true, "Renders simmed thorn's path")
    private val baseSpeed by NumberSetting("base speed mm/t", 340.0, 320..360, 0.1, "rahh")
    private val speedIncrease by NumberSetting("speed increase mm/t", 100, 0.0..300.0, 0.1, "rahh")
    private val conditionRadius by NumberSetting("point check radius", 0.1, 0.5..2.5, 0.05, "rahh")


    var positions = listOf<Vec3>()
    var simPositions = listOf<Vec3>()

    var lastLookAngle: Vec3? = null
    var lastLastLookAngle: Vec3? = null

    var simulatedThorn = Vec3(5.0, 84.0, 29.0)
    var displayedSimulatedThorn = simulatedThorn
    var simLastFrameNanos = 0L

    var stunTimer = 232
    var stunCounter = 0

    val points = listOf(
        Vec3(-6.3, 84.0, 26.9),
        Vec3(-14.9, 84.0, 18.9),
        Vec3(-18.5, 84.0, 6.5),
        Vec3(-17.1, 84.0, -6.2),
        Vec3(-9.1, 84.0, -14.7),
        Vec3(3.5, 84.0, -18.5),
        Vec3(16.2, 84.0, -17.2),
        Vec3(24.7, 84.0, -8.9),
        Vec3(28.5, 84.0, 3.5),
        Vec3(27.0, 84.0, 16.4),
        Vec3(19.0, 84.0, 24.8),
        Vec3(6.5, 84.0, 28.5),
    )
    var activePointIndex = 0

    fun thornSpeed (): Double {
        return baseSpeed / 1000.0 + speedIncrease*stunCounter / 1000.0
    }

    fun selectNextPoint() {
        // stunCounter actually counts double the amount of stuns compared to how many actually happened
        // because the packet is sent twice for every stun
        val clockwise = stunCounter % 4 == 0
        debugMessage(stunCounter)
        if (clockwise) {
            activePointIndex++
        } else {
            activePointIndex--
        }
        if (activePointIndex == -1) activePointIndex = 11
        if (activePointIndex == 12) activePointIndex = 0
    }

    fun simThorn () {
        if (stunTimer == -1) {
            val target = points[activePointIndex]
            val direction = target.subtract(simulatedThorn).normalize()
            val speed = thornSpeed()
            val velocity = direction.scale(speed)
            simulatedThorn = simulatedThorn.add(velocity)

            if (simulatedThorn.distanceTo(target) < conditionRadius) {
                selectNextPoint()
            }
        }
    }
    var thornSpeed = 0.0

    private fun colorForSpeed(speed: Double): Color{
        val redSpeed = 0.35
        val blueSpeed = 0.27

        // t = 0 at red, t = 1 at blue
        val t = ((redSpeed - speed) / (redSpeed - blueSpeed))
            .coerceIn(0.0, 1.0)
            .toFloat()

        return Color(
            (255 * (1f - t)).toInt(),
            0,
            (255 * t).toInt()
        )
    }

    private fun round(number: Double, dp: Int): Double {
        return (number * 10.0.pow(dp)).roundToInt() / 10.0.pow(dp)
    }


    init {
        onReceive<ClientboundMoveEntityPacket> {
            val packet = it.packet as? ClientboundMoveEntityPacket ?: return@onReceive
            val level = mc.level ?: return@onReceive
            val entity = packet.getEntity(level) ?: return@onReceive
            if (entity is Ghast) {
                //thornSpeed = entity.position().subtract(lastThornPos).length()
                //lastThornPos = entity.position()
                thornSpeed = entity.deltaMovement.length()
            }
        }

        on<RenderExtractEvent> {
            if (!BossLifecycle.inBoss()) return@on

            if (renderPath) {
                if (colorPathBySpeed && positions.size >= 2) {
                    for (i in 0 until positions.lastIndex) {
                        val speed = positions[i + 1].subtract(positions[i]).length()
                        drawLine(
                            listOf(positions[i], positions[i + 1]),
                            colorForSpeed(speed),
                            depth = false
                        )
                    }
                } else {
                    drawLine(positions, Colors.MINECRAFT_DARK_RED, depth = false)
                }
            }
            if (renderSimPath) drawLine(simPositions, Colors.MINECRAFT_LIGHT_PURPLE, depth = false)
            if (thornSim) {
                val target = simulatedThorn
                val now = System.nanoTime()

                if (simLastFrameNanos == 0L) {
                    displayedSimulatedThorn = target
                } else {
                    val dt = ((now - simLastFrameNanos) / 1_000_000_000.0).coerceAtMost(0.1)
                    val alpha = 1.0 - kotlin.math.exp(-12.0 * dt)

                    displayedSimulatedThorn = displayedSimulatedThorn.add(
                        target.subtract(displayedSimulatedThorn).scale(alpha)
                    )
                }

                simLastFrameNanos = now

                val pos = displayedSimulatedThorn
                drawFilledBox(AABB(pos.subtract(0.5), pos.add(0.5)), Colors.MINECRAFT_LIGHT_PURPLE)
            }
            val thorn = Mobs.thorn ?: return@on
            if (renderSpeed) {
                drawText(thornSpeed.toString(), thorn.renderPos, 10f, false)
            }

            if (directionLine) {
                val lookAngle = if (constantY) Vec3(thorn.lookAngle.x, 0.0, thorn.lookAngle.z) else thorn.lookAngle
                val direction = thorn.position().add(lookAngle.scale(200.0))
                val opposite = thorn.position().add(lookAngle.scale(-200.0))
                val points = if (bidirectionalLine) listOf(opposite, direction)
                else listOf(thorn.position(), direction)

                drawLine(points, Colors.MINECRAFT_AQUA, depth = false)
            }
        }

        on<TickEvent.Server> {
            if (!BossLifecycle.inBoss()) return@on

            if (stunTimer > -1) {
                stunTimer--
            }

            simThorn()

            simPositions = simPositions + listOf(simulatedThorn)

            val thorn = Mobs.thorn ?: return@on

            if (printSpeed && Timers.timer % 100 == 0 && positions.size >= 301) {
                val last100 = positions.subList(positions.size - 301, positions.size - 1)

                val averageDistance = last100
                    .zipWithNext()
                    .map { (a, b) -> a.distanceTo(b) }
                    .average()

                val rounded = round(averageDistance * 1000) / 1000

                modMessage("Thorn Speed last 15s: $rounded (make sure thorn stays in render for this to be accurate)")
            }

            positions = positions + listOf(thorn.position())


            if (thorn.lookAngle == lastLookAngle && lastLookAngle != lastLastLookAngle && printLineEquationAtCorner) {
                val slope = thorn.lookAngle.z / thorn.lookAngle.x
                modMessage("y = ${round(slope, 5)}(x + ${round(-thorn.x, 5)}) + ${round(thorn.z, 5)}")
            }
            if (thorn.lookAngle == lastLookAngle && lastLookAngle != lastLastLookAngle && printPosAtCorner) {
                modMessage("thorn PosAtCorner: ${(thorn.x * 100).roundToInt() / 100.0} ${thorn.y} ${(thorn.z * 100).roundToInt() / 100.0}")
            }
            if (thorn.lookAngle == lastLookAngle && lastLookAngle != lastLastLookAngle && printPosAndDirectionAtCorner) {
                modMessage("thorn pos: ${(thorn.x * 1000).roundToInt() / 1000.0} ${(thorn.z * 1000).roundToInt() / 1000.0}, dir: ${(thorn.lookAngle.x * 1000000).roundToInt() / 1000000.0} ${(thorn.lookAngle.z * 1000000).roundToInt() / 1000000.0}")
            }
            lastLastLookAngle = lastLookAngle
            lastLookAngle = thorn.lookAngle
        }

        onReceive<ClientboundHurtAnimationPacket> {
            if (!BossLifecycle.inBoss()) return@onReceive
            val entity = OdinMod.mc.level!!.getEntity(this.id)
            if (entity is Ghast) {
                stunTimer = 84
                stunCounter++
                if (stunCounter % 2 == 0) { // make sure that this only happens 1 time per stun
                    selectNextPoint()
                }
            }
        }

        on<LevelEvent.Load> {
            positions = listOf()
            stunTimer = 232
            activePointIndex = 0
            stunCounter = 0
        }
    }
}
