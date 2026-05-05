package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.Webserver.startWebserver
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt
import com.github.luben.zstd.ZstdOutputStream


object Logging: Module(
    name = "Logging",
    description = "Logging for m4 <3",
    category = Category.M4
) {
    private var ended = false // prevents logging multiple times
    private var pendingEnd = false

    private val startWebserver by ActionSetting("Start Webserver", "Starts the m4 webserver website thingy") {
        startWebserver()
    }

    val eventBuffer = ArrayList<Event>(256)

    var trackedById = HashMap<Int, TrackedMob>()
    var uuidToId = HashMap<UUID, Int>()

    var nextId: Int = 0

    var currentTick = 0
    var lastTick = 0

    private fun quantize(value: Double): Int {
        return (value * 20.0).roundToInt()
    }

    class TrackedMob {
        var id: Int = 0
        var uuid: UUID? = null
        var lastX: Int = 0
        var lastY: Int = 0
        var lastZ: Int = 0
    }

    sealed class Event {

        class MoveSmall(val id: Int, val dx: Int, val dy: Int, val dz: Int) : Event()

        class MoveMedium(val id: Int, val dx: Int, val dy: Int, val dz: Int) : Event()

        class MoveLarge(val id: Int, val dx: Int, val dy: Int, val dz: Int) : Event()
        class Spawn(val id: Int, val type: Int, val x: Int, val y: Int, val z: Int) : Event()
        class Despawn(val id: Int) : Event()
        class Kills(val kills: Int) : Event()
    }

    val TRACKED_TYPES = setOf(
        EntityType.BAT,
        EntityType.CHICKEN,
        EntityType.RABBIT,
        EntityType.SHEEP,
        EntityType.COW,
        EntityType.WOLF,
        EntityType.GHAST
    )

    private val searchBox = AABB(-36.0, 68.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    private fun writeVarInt(value: Int) {
        var v = value
        while (true) {
            if ((v and 0xFFFFFF80.toInt()) == 0) {
                out!!.writeByte(v)
                return
            } else {
                out!!.writeByte((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    private fun zigZagEncode(value: Int): Int {
        return (value shl 1) xor (value shr 31)
    }

    const val EVENT_MOVE_SMALL = 1
    const val EVENT_MOVE_MEDIUM = 2
    const val EVENT_MOVE_LARGE = 3
    const val EVENT_SPAWN = 4
    const val EVENT_DESPAWN = 5
    const val EVENT_KILLS = 6

    val logsFolder = File(mc.gameDirectory, "m4logs/logs").apply { mkdirs() }

    var out: DataOutputStream? = null

    @Throws(IOException::class)
    fun writeKills(kills: Int) {
        eventBuffer.add(Event.Kills(kills))
    }

    @Throws(IOException::class)
    private fun writeMove(id: Int, dx: Int, dy: Int, dz: Int) {

        val event = when {
            dx in -8..7 && dy in -8..7 && dz in -8..7 -> {
                Event.MoveSmall(id, dx, dy, dz)
            }

            dx in -128..127 && dy in -128..127 && dz in -128..127 -> {
                Event.MoveMedium(id, dx, dy, dz)
            }

            else -> {
                Event.MoveLarge(id, dx, dy, dz)
            }
        }

        eventBuffer.add(event)
    }

    @Throws(IOException::class)
    private fun writeSpawn(id: Int, type: Int, x: Int, y: Int, z: Int) {
        eventBuffer.add(Event.Spawn(id, type, x, y, z))
    }

    @Throws(IOException::class)
    private fun writeDespawn(id: Int) {
        eventBuffer.add(Event.Despawn(id))
    }

    val TYPE_IDS = mapOf(
        EntityType.BAT to 0,
        EntityType.CHICKEN to  1,
        EntityType.RABBIT to 2,
        EntityType.SHEEP to 3,
        EntityType.COW to 4,
        EntityType.WOLF to 5,
        EntityType.GHAST to 6,
        DungeonClass.Berserk to 7,
        DungeonClass.Mage to 8,
        DungeonClass.Archer to 9,
        DungeonClass.Healer to 10,
        DungeonClass.Tank to 11,
        "Spirit Bear" to 12
    )

    private fun flushTick() {
        if (eventBuffer.isEmpty()) return

        val delta = currentTick - lastTick

        writeVarInt(delta)
        writeVarInt(eventBuffer.size)

        for (event in eventBuffer) {
            when (event) {

                is Event.MoveSmall -> {
                    writeVarInt(EVENT_MOVE_SMALL)
                    writeVarInt(event.id)

                    val packed =
                        ((event.dx + 8) and 0xF) or
                                (((event.dy + 8) and 0xF) shl 4) or
                                (((event.dz + 8) and 0xF) shl 8)

                    out!!.writeShort(packed)
                }

                is Event.MoveMedium -> {
                    writeVarInt(EVENT_MOVE_MEDIUM)
                    writeVarInt(event.id)

                    out!!.writeByte(event.dx)
                    out!!.writeByte(event.dy)
                    out!!.writeByte(event.dz)
                }

                is Event.MoveLarge -> {
                    writeVarInt(EVENT_MOVE_LARGE)
                    writeVarInt(event.id)

                    writeVarInt(zigZagEncode(event.dx))
                    writeVarInt(zigZagEncode(event.dy))
                    writeVarInt(zigZagEncode(event.dz))
                }

                is Event.Spawn -> {
                    writeVarInt(EVENT_SPAWN)
                    writeVarInt(event.id)
                    writeVarInt(event.type)
                    writeVarInt(event.x)
                    writeVarInt(event.y)
                    writeVarInt(event.z)
                }

                is Event.Despawn -> {
                    writeVarInt(EVENT_DESPAWN)
                    writeVarInt(event.id)
                }

                is Event.Kills -> {
                    writeVarInt(EVENT_KILLS)
                    writeVarInt(event.kills)
                }
            }
        }

        lastTick = currentTick
        eventBuffer.clear()
    }

    var logFile: File? = null

    init {
        on<TickEvent.Server> {
            try {
                if (ended) return@on
                if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(4)) return@on
                currentTick++
                if (out == null) {
                    logFile = File(logsFolder, "${System.currentTimeMillis()}.bin")
                    val zstd = ZstdOutputStream(logFile!!.outputStream().buffered(64*1024))
                    zstd.setLevel(3)
                    out = DataOutputStream(zstd)
                    out!!.writeInt(0x4D344C31)
                    out!!.writeByte(1) // version
                }

                val seenThisTick = HashSet<UUID>()

                val allEntities = OdinMod.mc.level?.getEntities(null, searchBox) ?: emptyList()

                for (entity in allEntities) {
                    if (!entity.isAlive || entity.isInvisible) continue
                    var type = 0
                    if (!TRACKED_TYPES.contains(entity.type)) {
                        if (entity is Player) {
                            val name = entity.gameProfile.name
                            type = when {
                                name.startsWith("Spirit Bear") -> TYPE_IDS["Spirit Bear"]!!
                                DungeonUtils.dungeonTeammates.any { it.name == name } -> TYPE_IDS[DungeonUtils.dungeonTeammates.find { it.name == name }!!.clazz]!!
                                else -> continue
                            }
                        } else {
                            continue
                        }
                    } else {
                        type = TYPE_IDS[entity.type]!!
                    }

                    val uuid: UUID = entity.getUUID()
                    seenThisTick.add(uuid)

                    val id: Int = uuidToId.computeIfAbsent(uuid) { nextId++ }

                    var tracked = trackedById[id]

                    val x = quantize(entity.x)
                    val y = quantize(entity.y)
                    val z = quantize(entity.z)

                    if (tracked == null) {
                        // SPAWN
                        tracked = TrackedMob()
                        tracked.id = id
                        tracked.uuid = entity.uuid
                        tracked.lastX = x
                        tracked.lastY = y
                        tracked.lastZ = z

                        trackedById[id] = tracked

                        writeSpawn(id, type, x, y, z)
                    } else {
                        val dx = x - tracked.lastX
                        val dy = y - tracked.lastY
                        val dz = z - tracked.lastZ

                        if (dx != 0 || dy != 0 || dz != 0) {
                            writeMove(id, dx, dy, dz)

                            tracked.lastX = x
                            tracked.lastY = y
                            tracked.lastZ = z
                        }
                    }
                }


                // DESPAWN detection
                val it = trackedById.entries.iterator()

                while (it.hasNext()) {
                    val entry = it.next()
                    val tracked = entry.value

                    val uuid = tracked.uuid ?: continue

                    if (!seenThisTick.contains(uuid)) {
                        writeDespawn(tracked.id)

                        uuidToId.remove(uuid)
                        it.remove()
                    }
                }

                flushTick()
                if (pendingEnd && !ended) {
                    out?.flush()
                    out?.close()
                    out = null
                    ended = true
                    logFile = null
                    modMessage("Logged boss")
                }
            } catch (e: ConcurrentModificationException) {
                modMessage("concurrency error at ${M4State.timer}")
                print(e.message)
                eventBuffer.clear()
            }
        }

        on<WorldEvent.Load> {
            trackedById.clear()
            uuidToId.clear()
            nextId = 0
            currentTick = 0
            lastTick = 0
            pendingEnd = false
            ended = false

            out?.close()
            out = null
            eventBuffer.clear()
            logFile?.delete()
        }

        on<ChatPacketEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on
            if (M4State.endRegex.matches(value) && !ended && !pendingEnd) {
                pendingEnd = true
            }
        }
    }
}