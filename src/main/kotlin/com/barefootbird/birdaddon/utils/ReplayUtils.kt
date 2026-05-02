package com.barefootbird.birdaddon.utils

import com.github.luben.zstd.ZstdInputStream
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.modMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.io.DataInputStream
import java.io.File
import kotlin.collections.emptyList

data class EntityState(
    val id: Int,
    val type: Int,
    var x: Int,
    var y: Int,
    var z: Int
)

data class EntityStateDouble(
    val id: Int,
    val type: Int,
    var x: Double,
    var y: Double,
    var z: Double
)

data class Snapshot(
    val tick: Int,
    val entities: Map<Int, EntityState>,
    val kills: Int
)

class ReplayDecoder(file: File) {

    private var currentKills = 0

    private val input = DataInputStream(ZstdInputStream(file.inputStream().buffered()))

    val snapshots = mutableListOf<Snapshot>()

    private val currentEntities = HashMap<Int, EntityState>()
    private var currentTick = 0

    init {
        decode()
    }

    private fun decode() {
        val magic = input.readInt()
        if (magic != 0x4D344C31) {
            throw RuntimeException("Invalid M4 file magic")
        }

        val version = input.readUnsignedByte()
        if (version != 1) {
            throw RuntimeException("Unsupported version: $version")
        }

        snapshots.add(Snapshot(0, emptyMap(), 0))

        while (input.available() > 0) {
            stepBlock()
        }

        snapshots.add(Snapshot(currentTick, deepCopyState(), currentKills))
    }

    private fun stepBlock() {
        val deltaTicks = readVarInt(input)
        val eventCount = readVarInt(input)

        currentTick += deltaTicks

        repeat(eventCount) {
            when (val eventType = readVarInt(input)) {
                1 -> handleMoveSmall()
                2 -> handleMoveMedium()
                3 -> handleMoveLarge()
                4 -> handleSpawn()
                5 -> handleDespawn()
                6 -> handleKills()
                else -> throw RuntimeException("Unknown event type $eventType")
            }
        }

        snapshots.add(
            Snapshot(currentTick, deepCopyState(), currentKills)
        )
    }

    private fun handleMoveSmall() {
        val id = readVarInt(input)

        val packed = input.readUnsignedShort()

        val dx = ((packed and 0xF) - 8)
        val dy = (((packed shr 4) and 0xF) - 8)
        val dz = (((packed shr 8) and 0xF) - 8)

        val e = currentEntities[id] ?: return

        currentEntities[id] = e.copy(
            x = e.x + dx,
            y = e.y + dy,
            z = e.z + dz
        )
    }

    private fun handleMoveMedium() {
        val id = readVarInt(input)

        val dx = input.readByte().toInt()
        val dy = input.readByte().toInt()
        val dz = input.readByte().toInt()

        val e = currentEntities[id] ?: return

        currentEntities[id] = e.copy(
            x = e.x + dx,
            y = e.y + dy,
            z = e.z + dz
        )
    }

    private fun handleMoveLarge() {
        val id = readVarInt(input)

        val dx = zigZagDecode(readVarInt(input))
        val dy = zigZagDecode(readVarInt(input))
        val dz = zigZagDecode(readVarInt(input))

        val e = currentEntities[id] ?: return

        currentEntities[id] = e.copy(
            x = e.x + dx,
            y = e.y + dy,
            z = e.z + dz
        )
    }

    private fun handleSpawn() {
        val id = readVarInt(input)
        val type = readVarInt(input)

        val x = readVarInt(input)
        val y = readVarInt(input)
        val z = readVarInt(input)

        currentEntities[id] = EntityState(id, type, x, y, z)
    }

    private fun handleDespawn() {
        val id = readVarInt(input)
        currentEntities.remove(id)
    }

    private fun handleKills() {
        currentKills = readVarInt(input)
    }

    private fun deepCopyState(): Map<Int, EntityState> {
        val out = HashMap<Int, EntityState>(currentEntities.size)

        for ((id, e) in currentEntities) {
            out[id] = e.copy()
        }

        return out
    }

    private fun readVarInt(input: DataInputStream): Int {
        var numRead = 0
        var result = 0

        while (true) {
            val byte = input.readUnsignedByte()
            val value = byte and 0x7F

            result = result or (value shl (7 * numRead))

            numRead++

            if ((byte and 0x80) == 0) break
            if (numRead > 5) throw RuntimeException("VarInt too big")
        }

        return result
    }

    private fun zigZagDecode(value: Int): Int {
        return (value ushr 1) xor -(value and 1)
    }
}

class LogEntry(val file: File) : ObjectSelectionList.Entry<LogEntry>() {

    override fun renderContent(
        graphics: GuiGraphics,
        i: Int,
        j: Int,
        bl: Boolean,
        f: Float
    ) {
        graphics.drawString(
            mc.font,
            file.nameWithoutExtension,
            x + 5,
            y + 5,
            -0x01ffff
        )
        //val startColor = -0xff0100 // Green
        //val endColor = -0xffff01 // Blue
        //graphics.fillGradient(x, y, x + this.width, y + this.height, startColor, endColor);
    }

    override fun getNarration(): Component {
        return Component.literal("Log file: ${file.name}, last modified: ${file.lastModified()}")
    }
}

class LogListWidget(
    client: Minecraft,
    width: Int,
    top: Int,
    bottom: Int,
    itemHeight: Int
) : ObjectSelectionList<LogEntry>(client, width, bottom, top, itemHeight) {
    init {
        loadLogs()
    }

    private fun loadLogs() {
        val dir = File(mc.gameDirectory, "m4logs/logs").apply { mkdirs() }

        try {

            dir.listFiles().filter{
                it.name.endsWith(".bin")
            }.sorted().reversed().forEach {
                addEntry(LogEntry(it))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getRowWidth(): Int = width - 20
}

class LogSelectScreen(
    private val onReplaySelected: (File) -> Unit
) : Screen(Component.literal("Select Log")) {

    private lateinit var list: LogListWidget

    override fun init() {
        super.init()
        list = LogListWidget(minecraft!!, width, 32, height - 150, 20)
        addRenderableWidget(list)

        addRenderableWidget(
            Button.builder(Component.literal("Replay")) {
                val selected = list.selected
                if (selected != null) {
                    onReplaySelected(selected.file)
                    minecraft!!.setScreen(null) // close the screen
                }
            }.bounds(width / 2 - 100, height - 70, 200, 20).build()
        )

        val renameField = EditBox(mc.font, width / 2 - 225, height - 28, 200, 20, Component.literal("New Name"))

        addRenderableWidget(
            renameField
        )

        addRenderableWidget(
            Button.builder(Component.literal("Rename")) {
                val selected = list.selected
                val newName = renameField.value.trim()
                if (selected != null && newName.isNotEmpty()) {
                    val oldFile = selected.file
                    val newFile = File(oldFile.parentFile, "$newName.bin")

                    if (File(oldFile.parentFile, "$newName.bin").exists()) {
                        modMessage("File with this name already exists")
                        return@builder
                    }

                    if (oldFile.renameTo(newFile)) {
                        removeWidget(list)
                        list = LogListWidget(minecraft!!, width, 32, height - 150, 20)
                        addRenderableWidget(list)
                        //list.children().remove(selected)
                        //list.addNewEntry(LogEntry(newFile))
                        modMessage("Renamed successfully")
                    } else {
                        modMessage("Failed to rename")
                    }
                }
            }.bounds(width / 2 + 25, height - 28, 200, 20).build()
        )

    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(guiGraphics, mouseX, mouseY, delta)
    }
}

class ReplayRuntime {

    var snapshots: List<Snapshot> = emptyList()

    var currentIndex = 0
    var lastIndex = 0

    var lastTicksEntities = HashMap<Int, EntityStateDouble>()
    val renderEntities = HashMap<Int, EntityStateDouble>()

    fun load(decodedSnapshots: List<Snapshot>) {
        reset()

        snapshots = decodedSnapshots
        lastIndex = snapshots[snapshots.size - 1].tick
        currentIndex = 0

        if (snapshots.isNotEmpty()) {
            applySnapshot(1)
        }
    }

    fun seek(index: Int) {
        applySnapshot(index)
    }

    fun stepForward() {
        applySnapshot(currentIndex + 1)
    }

    fun stepBackward() {
        applySnapshot(currentIndex - 1)
    }

     fun getSnapshot(tick: Int): Snapshot? {
        var result: Snapshot? = null

        for (snap in snapshots) {
            if (snap.tick >= tick) break
            result = snap
        }

        return result
    }

    private fun applySnapshot(tick: Int) {
        val snap = getSnapshot(tick) ?: return
        val prevSnap = getSnapshot(currentIndex)

        lastTicksEntities = HashMap(
            renderEntities.mapValues { (_, value) -> value.copy()
        })

        currentIndex = tick

        val prevEntities = prevSnap?.entities ?: emptyMap()
        val newEntities = snap.entities

        val newIds = newEntities.keys
        val oldIds = prevEntities.keys

        for (id in oldIds) {
            if (id !in newIds) {
                renderEntities.remove(id)
            }
        }

        for ((id, state) in newEntities) {

            val existing = renderEntities[id]

            val x = state.x / 20.0 + 2
            val y = state.y / 20.0 + 41
            val z = state.z / 20.0 + 2

            if (existing == null) {
                renderEntities[id] = EntityStateDouble(id, state.type, x, y, z)

            } else {
                existing.x = x
                existing.y = y
                existing.z = z
            }
        }
    }

    private fun reset() {
        renderEntities.clear()

        snapshots = emptyList()
        currentIndex = 0
    }
}