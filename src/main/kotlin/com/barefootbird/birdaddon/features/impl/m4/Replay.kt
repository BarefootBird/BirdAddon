package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.LogSelectScreen
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.M4State.onCgm4
import com.barefootbird.birdaddon.utils.ReplayDecoder
import com.barefootbird.birdaddon.utils.ReplayRuntime
import com.barefootbird.birdaddon.utils.modMessage
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.toFixed
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.io.File
import kotlin.math.abs
import kotlin.text.toFloatOrNull

object Replay : Module(
    name = "Replay",
    description = "Replays m4!!",
    category = Category.M4
) {
    var runtime: ReplayRuntime? = null
    var loaded = false
    var playing = false
    var playSpeed = 1f
    var stepAccumulator = 0f

    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    private val showTimer by HUD("Show timer", desc = "shows a timer in the replay", true) { example ->
        when {
            example -> "§e30.45s"
            !onCgm4 -> null
            else -> runtime?.currentIndex?.div(20.0)?.toFixed(2)
        }?.let { text ->
            textDim(text, 0, 0, Colors.MINECRAFT_RED)
        } ?: (0 to 0)
    }

    private val bearTimer by HUD("Bear timer", desc = "shows a bear timer in the replay", true) { example ->
        var text: String? = null
        if (!onCgm4) text = null

        if (example) text = "§e1.45s"

        if (runtime?.renderEntities?.any { e -> e.value.type == 12 } == true && text == null) text = "§c!"

        val kills = runtime?.currentIndex?.let { runtime?.getSnapshot(it)?.kills }
        if (kills != null && text == null) {
            if (kills < M4State.maxKills) text = "§a${kills}"
            if (kills == M4State.maxKills) {
                var tickWithBearNotSpawning: Int? = null
                var counter = 0
                val currentTime = runtime!!.currentIndex
                while (tickWithBearNotSpawning == null && counter < 100) {
                    counter++
                    val thisTicksKills = runtime?.getSnapshot(currentTime - counter)?.kills
                    if (thisTicksKills != null && thisTicksKills != M4State.maxKills) {
                        tickWithBearNotSpawning = currentTime - counter
                    }
                }
                if (tickWithBearNotSpawning != null) {
                    val spawnTime = tickWithBearNotSpawning + 69
                    val timeTillSpawn = spawnTime - runtime!!.currentIndex
                    text = "§c${(timeTillSpawn / 20.0).toFixed(2)}"
                }
            }
        }
        if (text != null) {
            textDim(text, 0, 0, Colors.WHITE)
        } else {
            0 to 0
        }
    }

    fun openLoaderScreen() {
        mc.execute {
            val screen = LogSelectScreen { file ->
                loadReplay(file.name)
            }
            mc.setScreen(screen)
        }
    }

    fun loadReplay(fileName: String) {
        val folder = File(mc.gameDirectory, "m4logs/logs")
        val file = File(folder, fileName)

        if (!file.exists() || !file.isFile) {
            modMessage("[Replay] Invalid file: ${file.name}")
            return
        }

        if (!file.canRead()) {
            modMessage("[Replay] Cannot read file: ${file.name}")
            return
        }

        if (!file.name.endsWith(".bin")) {
            modMessage("[Replay] Expected .bin file: ${file.name}")
            return
        }

        if (file.length() < 8) {
            modMessage("[Replay] File too small / corrupted: ${file.name}")
            return
        }

        val decoder = ReplayDecoder(file)

        runtime = ReplayRuntime().apply {
            load(decoder.snapshots)
        }

        loaded = true

        modMessage("[Replay] Loaded Boss ${runtime!!.lastIndex.div(20.0).toFixed(2)}s")
    }


    fun play() {
        playing = true
        modMessage("[Replay] Playing")
    }

    fun pause() {
        playing = false
        modMessage("[Replay] Paused")
    }

    fun setPlaySpeed(speed: String) {
        val parsed = speed.toFloatOrNull()

        if (parsed == null) {
            modMessage("[Replay]: invalid speed")
            return
        }

        playSpeed = parsed
        modMessage("[Replay] Play speed: $speed")
    }

    fun step() {
        val r = runtime ?: return modMessage("[Replay] Load a replay first")
        r.stepForward()
    }

    fun seek(input: String) {
        val r = runtime ?: return modMessage("[Replay] Load a replay first")

        val ticks = parseTimeToTicks(input)
        if (ticks < 0) {
            modMessage("[Replay] Invalid time format: $input")
            return
        }

        r.seek(ticks)

        modMessage("[Replay] Seeked to $ticks ticks")
    }

    private fun parseTimeToTicks(input: String): Int {
        val trimmed = input.trim().lowercase()

        return try {
            when {
                trimmed.endsWith("s") -> {
                    val sec = trimmed.dropLast(1).toDouble()
                    (sec * 20).toInt()
                }
                else -> trimmed.toInt()
            }
        } catch (_: Exception) {
            -1
        }
    }


    init {
        on<TickEvent.Start> {
            if (!playing) return@on

            stepAccumulator += playSpeed

            while (stepAccumulator >= 1f) {
                val reachedEnd = runtime!!.currentIndex == runtime!!.lastIndex
                stepAccumulator -= 1f
                runtime!!.stepForward()
                if (reachedEnd) {
                    modMessage("[Replay]: reached end of replay")
                    playing = false
                }
            }

            while (stepAccumulator <= -1f) {
                val reachedStart = runtime!!.currentIndex == 0
                runtime!!.stepBackward()
                stepAccumulator += 1f
                if (reachedStart) {
                    modMessage("[Replay]: reached start of replay")
                    playing = false
                }
            }
        }

        on<WorldEvent.Load> {
            runtime = null
            loaded = false
            playing = false
        }

        on<RenderEvent.Extract> {
            val r = runtime ?: return@on
            r.renderEntities.forEach {
                val e = it.value

                data class EntityRenderInfo(
                    val width: Float,
                    val height: Float,
                    val color: Color
                )

                val info = when (e.type) {
                    0 -> EntityRenderInfo(EntityType.BAT.width, EntityType.BAT.height, Highlight.batColor)
                    1 -> EntityRenderInfo(EntityType.CHICKEN.width, EntityType.CHICKEN.height, Highlight.chickenColor)
                    2 -> EntityRenderInfo(EntityType.RABBIT.width, EntityType.RABBIT.height, Highlight.rabbitColor)
                    3 -> EntityRenderInfo(EntityType.SHEEP.width, EntityType.SHEEP.height, Highlight.sheepColor)
                    4 -> EntityRenderInfo(EntityType.COW.width, EntityType.COW.height, Highlight.cowColor)
                    5 -> EntityRenderInfo(EntityType.WOLF.width, EntityType.WOLF.height, Highlight.wolfColor)
                    6 -> EntityRenderInfo(EntityType.GHAST.width, EntityType.GHAST.height, Highlight.thornColor)
                    7 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_RED) // bers
                    8 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_BLUE) // mage
                    9 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_GOLD) // arch
                    10 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_LIGHT_PURPLE) // heal
                    11 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_GREEN) // tank
                    12 -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_DARK_PURPLE) // bear
                    else -> EntityRenderInfo(EntityType.PLAYER.width, EntityType.PLAYER.height, Colors.MINECRAFT_GRAY) // other
                }


                val w = info.width
                val h = info.height
                val color = info.color

                var x = e.x
                var y = e.y
                var z = e.z

                if (playing && r.lastTicksEntities.contains(it.key) && abs(playSpeed) >= 1) {
                    val last = r.lastTicksEntities[it.key]!!
                    // interpolation
                    val tickDelta = mc.deltaTracker.getGameTimeDeltaPartialTick(false)
                    x = last.x + (e.x - last.x) * tickDelta
                    y = last.y + (e.y - last.y) * tickDelta
                    z = last.z + (e.z - last.z) * tickDelta
                }

                val bb =  AABB(x - w / 2, y, z - w / 2.0, x + w / 2, y + h, z + w / 2)

                drawStyledBox(bb, color, renderStyle, false)
                if (e.type in 7..11) {
                    val text = when (e.type) {
                        7 -> "Bers"
                        8 -> "Mage"
                        9 -> "Arch"
                        10 -> "Heal"
                        11 -> "Tank"
                        else -> ""
                    }
                    drawText(text, Vec3(x, y + 2.2, z), 2.0F, false)
                }
            }
        }
    }
}