package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
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
import com.barefootbird.birdaddon.utils.Islands.onCgm4
import com.barefootbird.birdaddon.utils.Islands.onM4Miku
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.utils.render.drawStyledBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.setClipboardContent
import net.minecraft.world.phys.Vec3
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.math.cos
import kotlin.math.sin


object Waypoints: Module(
    name = "Waypoints",
    description = "Waypoints for m4",
    category = Category.M4
) {
    private val renderStyle by SelectorSetting(
        "Render Style",
        "Outline",
        listOf("Filled", "Outline", "Filled Outline"),
        desc = "Style of the box."
    )
    private val depth by BooleanSetting("depth", true, "depth")
    private val showOnCgm4 by BooleanSetting("Show on cgm4", true, "Shows the waypoints on cgm4's is")
    private val showOnM4Miku by BooleanSetting("Show on m4miku", true, "Shows the waypoints on m4miku's is")
    private val loadCommand by ActionSetting(
        "Load Waypoints from file",
        "Load Waypoints from .minecraft/config/odin/addons/m4waypoints.json"
    ) { loadWaypoints() }

    private val bearSpawn by BooleanSetting("Bear Spawn Waypoint", true, "Shows the waypoint for bear spawn")
    private val bearSpawnOnMage by BooleanSetting(
        "Bear wp only on Mage",
        true,
        "Shows the waypoint for bear spawn"
    ).withDependency { bearSpawn }
    private val bearSpawnOnlyWhenBearIsSpawning by BooleanSetting(
        "Bear wp only when spawning",
        false,
        "Only shows the waypoint when the bear is spawning"
    ).withDependency { bearSpawn }
    private val bearSpawnColors by BooleanSetting(
        "Bear wp change colors",
        true,
        "Changes colors: red when bear is spawning, green when its spawning and you're looking at it, orange when you're too close to it"
    ).withDependency { bearSpawn }
    private val defaultColor by ColorSetting(
        "Default",
        Colors.MINECRAFT_BLUE,
        true,
        desc = "Color when bear isnt spawning"
    ).withDependency { bearSpawnColors }
    private val lookingAtColor by ColorSetting(
        "Looking at",
        Colors.MINECRAFT_GREEN,
        true,
        desc = "Color when bear is spawning and you're looking at it"
    ).withDependency { bearSpawnColors }
    private val notLookingAtColor by ColorSetting(
        "Not looking at",
        Colors.MINECRAFT_RED,
        true,
        desc = "Color when bear is spawning you're not looking at it"
    ).withDependency { bearSpawnColors }
    private val tooCloseColor by ColorSetting(
        "Too close",
        Colors.MINECRAFT_GOLD,
        true,
        desc = "Color when you're too close that you'd do a vanilla melee hit"
    ).withDependency { bearSpawnColors }

    private val bowPickupWaypoint by BooleanSetting(
        "Bow Pickup Waypoint",
        true,
        "Dynamic bow pickup waypoint that updates based on bear position"
    )
    private val bowPickupWaypointOnlyOnTank by BooleanSetting(
        "Bow wp only on tank",
        true,
        "Only shows the bow pickup waypoint when you're on tank class"
    ).withDependency { bowPickupWaypoint }

    val wpConfig = File(mc.gameDirectory, "config/odin/addons/m4waypoints.json")


    data class Waypoint(
        val pos: BlockPos,
        val clazz: String,
        val start: String,
        val end: String,
    )

    private val waypoints = mutableListOf<Waypoint>()

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun dungeonClassFromName(name: String): DungeonClass? {
        return when (name.trim().lowercase()) {
            "berserk", "bers", "b" -> DungeonClass.BERSERK
            "healer", "heal", "h" -> DungeonClass.HEALER
            "archer", "arch", "a" -> DungeonClass.ARCHER
            "mage", "m" -> DungeonClass.MAGE
            "tank", "t" -> DungeonClass.TANK
            else -> null
        }
    }

    fun exportWaypoints() {
        val exported: String? = try {
            Base64.encode(compress(gson.toJson(waypoints)))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (exported == null) {
            modMessage("Waypoints failed to export")
            return
        }

        setClipboardContent(exported)
        modMessage("Waypoints exported to clipboard")
    }

    private fun compress(input: String): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(input.toByteArray())
            }
            return byteArrayOutputStream.toByteArray()
        }
    }

    fun importWaypoints() {
        val clipboard = mc.keyboardHandler.clipboard.trim().trim { it == '\n' }
        val imported: List<Waypoint>? = try {
            gson.fromJson(
                decompress(Base64.decode(clipboard)),
                object : TypeToken<List<Waypoint>>() {}.type
            )
        } catch (_: Exception) {
            null
        }
        if (imported != null) {
            waypoints.clear()
            waypoints.addAll(imported)
            modMessage("Waypoints imported")
        } else {
            modMessage("Waypoints failed to import")
        }
    }

    private fun decompress(compressed: ByteArray): String {
        GZIPInputStream(compressed.inputStream()).use { gzipInputStream ->
            return gzipInputStream.bufferedReader().use { it.readText() }
        }
    }

    fun saveWaypoints() {
        if (!wpConfig.parentFile.exists()) {
            wpConfig.parentFile.mkdirs()
        }

        wpConfig.writeText(gson.toJson(waypoints))
    }

    fun loadWaypoints() {
        if (!wpConfig.exists()) return

        val type = object : TypeToken<MutableList<Waypoint>>() {}.type
        val loaded: MutableList<Waypoint> = gson.fromJson(wpConfig.readText(), type)

        waypoints.clear()
        waypoints.addAll(loaded)
    }

    fun removeWaypoint(pos: BlockPos) {
        var newPos = pos
        if (onCgm4) {
            newPos = BlockPos(pos.x - 2, pos.y - 41, pos.z -2) // cgm4's island is offset from actual m4
        } else if ((!DungeonUtils.inBoss || !DungeonUtils.inDungeons) && !onM4Miku) {
            modMessage("need to be in f4/m4 or on catgirlm4's/m4miku's is")
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
            if ((!DungeonUtils.inBoss || !DungeonUtils.inDungeons) && !onM4Miku) {
                modMessage("need to be in f4/m4 or on catgirlm4's/m4miku's is")
            }
        }

        val actualClass = dungeonClassFromName(clazz)?.name ?: clazz

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
        if (!onCgm4 && !onM4Miku && !(DungeonUtils.inBoss && DungeonUtils.isFloor(4))) return
        waypoints.forEach {
            val clazz = dungeonClassFromName(it.clazz) ?: return@forEach
            if (onCgm4 || onM4Miku || clazz == DungeonUtils.currentDungeonPlayer.clazz) {
                if (onCgm4 && showOnCgm4) {
                    renderWaypoint(BlockPos(it.pos.x + 2, it.pos.y + 41, it.pos.z + 2), clazz)
                } else if (onM4Miku && showOnM4Miku) {
                    renderWaypoint(BlockPos(it.pos.x, it.pos.y, it.pos.z), clazz)
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

    private fun RenderEvent.Extract.renderWaypoint (pos: BlockPos, clazz: DungeonClass) {
        val color = when (clazz) {
            DungeonClass.HEALER -> Colors.MINECRAFT_LIGHT_PURPLE
            DungeonClass.TANK -> Colors.MINECRAFT_GREEN
            DungeonClass.BERSERK -> Colors.MINECRAFT_DARK_RED
            DungeonClass.ARCHER -> Colors.MINECRAFT_GOLD
            DungeonClass.MAGE -> Colors.MINECRAFT_BLUE
            else -> return
        }
        drawStyledBox(box1x1(pos.x, pos.y, pos.z), color, renderStyle, depth)
    }

    private fun lookingAt (box: AABB, range: Double): Boolean {
        val eyePos = mc.player?.eyePosition ?: return false
        val lookVec = mc.player?.getViewVector(1.0F) ?: return false
        val end = eyePos.add(lookVec.scale(range))
        return box.clip(eyePos, end).isPresent
    }

    private fun RenderEvent.Extract.renderBearSpawn () {
        if (!(onCgm4 && showOnCgm4) && !(onM4Miku && showOnM4Miku) && bearSpawnOnlyWhenBearIsSpawning && (M4State.bearTimer == -1 || M4State.bearTimer == 0)) return

        val spawnSpot = M4State.bearSpawnSpot
        var bearSpawn = AABB(spawnSpot.x - 0.3, 70.4, spawnSpot.z - 0.3, spawnSpot.x + 0.3, 71.5, spawnSpot.z + 0.3)
        if (onCgm4) {
            bearSpawn = AABB(spawnSpot.x + 1.7, 111.4, spawnSpot.z + 1.7, spawnSpot.x + 2.3, 112.5, spawnSpot.x + 2.3)
        }

        val bearSpawnColor: Color = if (M4State.bearTimer == -1 || !bearSpawnColors) {
            defaultColor
        } else {

            if (lookingAt(bearSpawn, 3.0)) {
                tooCloseColor
            } else if (lookingAt(bearSpawn, 100.0)) {
                lookingAtColor
            } else {
                notLookingAtColor
            }

        }
        if ((onCgm4 && showOnCgm4) || (onM4Miku && showOnM4Miku) || (DungeonUtils.inBoss && DungeonUtils.isFloor(4))) {
            drawStyledBox(bearSpawn, bearSpawnColor, renderStyle, depth) // bear spawn
        }
    }


    private fun getBowSpawnSpot(): Vec3 {
        val defaultSpawn = Vec3(5.0, 69.0, 4.0)

        val bear = M4Mobs.bear ?: return defaultSpawn

        if (M4State.bearTimer != 0) return defaultSpawn

        val x = bear.x
        val z = bear.z

        return bowSpots.minByOrNull { spot ->
            val dx = spot.x - x
            val dz = spot.z - z
            dx * dx + dz * dz
        } ?: defaultSpawn
    }

    private fun RenderEvent.Extract.renderBowPickup () {
        if (!(onCgm4 && showOnCgm4) && !(onM4Miku && showOnM4Miku) && !DungeonUtils.inDungeons) return

        val closestSpot = getBowSpawnSpot()

        val points = 64
        val radius = 1.5

        val circlePoints = (0..points).map { i ->
            val angle = 2.0 * Math.PI * i / points

            Vec3(
                closestSpot.x + radius * cos(angle),
                closestSpot.y,
                closestSpot.z + radius * sin(angle)
            )
        }

        drawLine(circlePoints, Colors.MINECRAFT_GREEN, depth)

    }

    init {
        on<RenderEvent.Extract> {
            renderCustomWaypoints()
            if ((DungeonUtils.inBoss && DungeonUtils.isFloor(4)) || onCgm4 || onM4Miku) {
                if (bearSpawn && (!bearSpawnOnMage ||
                    onCgm4 ||
                    onM4Miku ||
                    DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.MAGE)) {
                    renderBearSpawn()
                }
                if (bowPickupWaypoint && (!bowPickupWaypointOnlyOnTank ||
                    onCgm4 ||
                    onM4Miku ||
                    DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.TANK
                    )) {
                    renderBowPickup()
                }
            }
        }

        on<LevelEvent.Load> {
            if (waypoints.isEmpty()) {
                loadWaypoints()
            }
        }


    }

    private val bowSpots = listOf(
        Vec3(5.0, 69.0, 4.0),
        Vec3(9.0, 69.0, 14.0),
        Vec3(16.0, 70.0, 7.0),
        Vec3(-2.0, 69.0, 0.0),
        Vec3(23.0, 69.0, 15.0),
        Vec3(3.0, 69.0, 21.0),
        Vec3(25.0, 69.0, 3.0),
        Vec3(24.0, 69.0, 25.0),
        Vec3(-2.0, 69.0, -6.0),
        Vec3(5.0, 71.0, -11.0),
        Vec3(12.0, 69.0, -4.0),
        Vec3(-1.0, 69.0, 11.0),
        Vec3(-18.0, 69.0, 6.0),
        Vec3(-15.0, 69.0, 24.0),
        Vec3(24.0, 69.0, -15.0),
        Vec3(-15.0, 69.0, -16.0),
        Vec3(-7.0, 69.0, 28.0),
        Vec3(0.0, 69.0, -15.0)
    )
}
