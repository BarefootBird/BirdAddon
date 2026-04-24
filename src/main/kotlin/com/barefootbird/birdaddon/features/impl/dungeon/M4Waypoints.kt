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
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawStyledBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import java.io.File


object M4Waypoints: Module(
    name = "M4 Waypoints",
    description = "Waypoints for m4",
    category = Category.BOSS
) {
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val depth by BooleanSetting("depth", true, "depth")
    private val bearSpawn by BooleanSetting("Bear Spawn Waypoint", true, "Shows the waypoint for bear spawn")
    private val bearSpawnOnMage by BooleanSetting("Bear wp Only on Mage", true, "Shows the waypoint for bear spawn").withDependency { bearSpawn }



    val wpConfig = File(mc.gameDirectory, "config/odin/addons/m4waypoints.json")

    var onCgm4 = false

    data class Waypoint(
        val pos: BlockPos,
        val clazz: String,
        val start: String,
        val end: String,
    )

    private val waypoints = mutableListOf<Waypoint>()

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun saveWaypoints() {
        if (!wpConfig.parentFile.exists()) {
            wpConfig.parentFile.mkdirs()
        }

        wpConfig.writeText(gson.toJson(waypoints))
    }

    fun removeWaypoint(pos: BlockPos) {
        var newPos = pos
        if (onCgm4) {
            newPos = BlockPos(pos.x - 2, pos.y - 41, pos.z -2) // cgm4's island is offset from actual m4
        } else {
            if (!DungeonUtils.inBoss || !DungeonUtils.inDungeons) {
                modMessage("need to be in f4/m4 or on catgirlm4's is")
            }
        }
        if (waypoints.removeIf { it.pos.x == newPos.x && it.pos.y == newPos.y && it.pos.z == newPos.z }) {
            modMessage("removed waypoint")
            saveWaypoints()
        } else {
            modMessage("no waypoint to remove")
        }
    }

    fun addWaypoint(pos: BlockPos, clazz: String, start: String, end: String) {
        var newPos = pos
        if (onCgm4) {
            newPos = BlockPos(pos.x - 2, pos.y - 41, pos.z -2) // cgm4's island is offset from actual m4
        } else {
            if (!DungeonUtils.inBoss || !DungeonUtils.inDungeons) {
                modMessage("need to be in f4/m4 or on catgirlm4's is")
            }
        }

        val classMap = mapOf(
            "berserk" to "Berserk",
            "bers" to "Berserk",
            "b" to "Berserk",
            "healer" to "Healer",
            "heal" to "Healer",
            "h" to "Healer",
            "archer" to "Archer",
            "arch" to "Archer",
            "a" to "Archer",
            "mage" to "Mage",
            "m" to "Mage",
            "tank" to "Tank",
            "t" to "Tank"
        )

        val actualClass = classMap[clazz.lowercase()] ?: clazz

        val waypoint = Waypoint(
            pos = newPos,
            clazz = actualClass,
            start = start,
            end = end
        )

        waypoints.add(waypoint)

        saveWaypoints()

        modMessage("Waypoint added at ${pos.x}, ${pos.y}, ${pos.z}")
    }

    fun loadWaypoints() {
        if (!wpConfig.exists()) return

        val type = object : TypeToken<MutableList<Waypoint>>() {}.type
        val loaded: MutableList<Waypoint> = gson.fromJson(wpConfig.readText(), type)

        waypoints.clear()
        waypoints.addAll(loaded)
    }

    fun shouldRenderNow(start: String, end: String): Boolean {
        return (eventPassed(start.lowercase()) && !eventPassed(end.lowercase()))
    }

    fun eventPassed(event: String): Boolean {
        if (event.startsWith("b")) {
            if (event.startsWith("boss")) {
                if (event.endsWith("start")) return true
                if (event.endsWith("end")) return false
            }
            val bearNum = event[1].digitToInt()
            if (event.endsWith("kill")) {
                return M4State.bearKillTimes.size >= bearNum
            }
            if (event.endsWith("spawn")) {
                return M4State.bearSpawnTimes.size >= bearNum
            }
            if (event.endsWith("spawnstart")) {
                return M4State.bearSpawnStartTimes.size >= bearNum
            }
        }
        if (event.endsWith("s")) {
            return M4State.timer / 20.0 > event.slice(0..<event.length - 1).toInt()
        }
        return false
    }

    fun RenderEvent.Extract.renderCustomWaypoints() {
        if (!onCgm4 && !(DungeonUtils.inBoss && DungeonUtils.inDungeons)) return
        waypoints.forEach {
            if (it.clazz == DungeonUtils.currentDungeonPlayer.clazz.toString() || onCgm4) {
                val clazz = enumValueOf<DungeonClass>(it.clazz)
                if (onCgm4) {
                    renderWaypoint(BlockPos(it.pos.x + 2, it.pos.y + 41, it.pos.z + 2), clazz)
                } else {
                    if (shouldRenderNow(it.start, it.end)) {
                        renderWaypoint(it.pos, DungeonUtils.currentDungeonPlayer.clazz)
                    }
                }
            }
        }
    }

    private fun box1x1 (x: Int, y: Int, z: Int): AABB {
        return AABB(x + 0.0, y + 0.0, z + 0.0, x+1.0, y+1.0, z+1.0)
    }

    private fun RenderEvent.Extract.renderWaypoint (x: Int, y: Int, z: Int, clazz: DungeonClass) {
        val color = when (clazz) {
            DungeonClass.Healer -> Colors.MINECRAFT_LIGHT_PURPLE
            DungeonClass.Tank -> Colors.MINECRAFT_GREEN
            DungeonClass.Berserk -> Colors.MINECRAFT_DARK_RED
            DungeonClass.Archer -> Colors.MINECRAFT_GOLD
            DungeonClass.Mage -> Colors.MINECRAFT_BLUE
            else -> return
        }
        drawStyledBox(box1x1(x, y, z), color, renderStyle, depth)
    }

    private fun RenderEvent.Extract.renderWaypoint (pos: BlockPos, clazz: DungeonClass) {
        renderWaypoint(pos.x, pos.y, pos.z, clazz)
    }

    private fun lookingAt (box: AABB): Boolean {

        val eyePos = mc.player?.eyePosition ?: return false
        val lookVec = mc.player?.getViewVector(1.0F) ?: return false
        val end = eyePos.add(lookVec.scale(100.0))
        return box.clip(eyePos, end).isPresent
    }

    private fun RenderEvent.Extract.renderBearSpawn () {
        var bearSpawn = AABB(5.75, 70.4, 5.75, 6.25, 71.5, 6.25)
        if (onCgm4) {
            bearSpawn = AABB(7.75, 111.4, 7.75, 8.25, 112.5, 8.25)
        }
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
    }

    private val teamRegex = "^team_(\\d+)$".toRegex()

    init {
        on<RenderEvent.Extract> {
            renderCustomWaypoints()
            if ((DungeonUtils.inBoss && DungeonUtils.isFloor(4)) || onCgm4) {
                if (bearSpawnOnMage && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.Mage && !onCgm4) return@on
                renderBearSpawn()
            }
        }

        on<WorldEvent.Load> {
            if (waypoints.isEmpty()) {
                loadWaypoints()
            }
            onCgm4 = false
        }

        onReceive<ClientboundSetPlayerTeamPacket> { event ->
            val packet = event.packet
            if (packet is ClientboundSetPlayerTeamPacket) {
                val opt = packet.parameters
                if (!opt.isPresent) return@onReceive
                val team = opt.get()
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return@onReceive
                if (!packet.name.matches(teamRegex)) return@onReceive
                val message = "${teamPrefix}${teamSuffix.trim()}".noControlCodes
                if (message.contains("catgirlm4")) {
                    onCgm4 = true
                }
            }
        }
    }
}