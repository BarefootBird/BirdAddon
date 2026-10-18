package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.modMessage
import com.barefootbird.birdaddon.utils.state.BearTracker
import com.barefootbird.birdaddon.utils.state.KillTracker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.toFixed
import com.barefootbird.birdaddon.utils.state.M4State
import com.barefootbird.birdaddon.utils.state.ParticleTrails
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import java.io.File

object SpiritBear: Module(
    name = "Spirit Bear",
    key = null,
    description = "Shows the state of the spirit bear spawns",
    category = Category.M4
) {

    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", true) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }
    val experimental by BooleanSetting("Experimental Bear Timer", false,"More accurate timer, but sometimes its really wrong")
    val experimentalRender by BooleanSetting("Render Experimental Prediction", false, "Renders the particle predictions used in the experimental timer").withDependency { experimental }
    val interpolation by BooleanSetting("Smooth Line", false, "Makes the progress of the line smooth").withDependency { experimentalRender }

    private val splitsHud by HUD("Spirit Bear Splits", "Displays Spirit Bear split times in the HUD.", true) { example ->
        val lines = splitLines(example)
        var width = 0
        var height = 0

        lines.forEachIndexed { index, line ->
            val (lineWidth, lineHeight) = textDim(line, 0, index * 9, Colors.WHITE)
            width = maxOf(width, lineWidth)
            height = (index * 9) + lineHeight
        }

        width to height
    }

    private val killPhaseText by StringSetting(
        "Kill Phase Text",
        $$"§6Bear $bear: §a$kills/$cap",
        desc = $$"HUD format for when bears are being killed",
        placeholder = $$"§6Bear $bear: §a$kills/$cap"
    )

    private val spawningText by StringSetting(
        "Spawning Text",
        $$"§6Bear $bear: §c$timer",
        desc = $$"HUD format for when bear is about to spawn",
        placeholder = $$"§6Bear $bear: §c$timer"
    )

    private val spawnedText by StringSetting(
        "Spawned Text",
        $$"§6Bear $bear: §4!",
        desc = $$"HUD format for when bear has spawned",
        placeholder = $$"§6Bear $bear: §4!"
    )

    private val splitText by StringSetting(
        "Split Text",
        $$"§6Bear $bear: §b$spawn Kill: $kill",
        96,
        desc = $$"HUD format for each bear split. Options: $bear, $spawnStart, $spawn, $death, $kill, $total",
        placeholder = $$"§6Bear $bear: §b$spawn Kill: $kill"
    )

    private val splitTotalText by StringSetting(
        "Split Total Text",
        $$"§7Total: §b$total",
        96,
        desc = $$"HUD format for the total boss time at the end of the splits. Options: $bear, $spawnStart, $spawn, $death, $kill, $total",
        placeholder = $$"§7Total: §b$total"
    )

    private val showTicks by BooleanSetting("Show Timer in Ticks", false, desc = "Changes the timer to be in ticks instead of seconds")

    private val decimals by NumberSetting("Decimals", 2, 1..2, 1, "How many decimals to show")

    private val personalBestsFile = File(mc.gameDirectory, "config/odin/addons/m4personalbests.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val personalBests = mutableMapOf<String, Float>()
    private val completedSplits = mutableListOf<BearSplit>()

    private data class BearSplit(
        val bear: Int,
        val spawnStartTime: Int,
        val spawnTime: Int,
        val deathTime: Int,
        val killTime: Int
    )

    private fun timerText(example: Boolean): String {

        if (example) return parseTemplate(killPhaseText, 1, "§c1.55s", 0, 30)

        if (!M4State.inBoss()) return ""

        val text = when {
            BearTracker.bearSpawning() -> spawningText
            BearTracker.bearSpawned() -> spawnedText
            else -> killPhaseText
        }

        val bear = BearTracker.bearKillTimes.size + 1
        val kills = KillTracker.kills
        val cap = KillTracker.maxKills

        val timerValue = when {
            BearTracker.bearSpawning() -> {
                val ticksLeft = M4State.bearTimer.toDouble()
                if (showTicks) {
                    ticksLeft.toFixed(0).padStart(2, '0')
                } else {
                    "${(ticksLeft / 20.0).toFixed(decimals)}s"
                }
            }
            else -> "n/a"
        }

        return parseTemplate(text, bear, timerValue, kills, cap)
    }

    private fun parseTemplate(template: String, bear: Int, timer: String, kills: Int, cap: Int): String {
        return template
            .replace($$"$bear", bear.toString())
            .replace($$"$timer", timer)
            .replace($$"$kills", kills.toString().padStart(2, '0'))
            .replace($$"$cap", cap.toString())
    }

    private fun splitLines(example: Boolean): List<String> {
        if (example) {
            return listOf(
                splitLine(1, "12.35s", "15.40s", "15.40s", formatKillTime(3), "15.40s"),
                splitTotalLine(1, "12.35s", "15.40s", "15.40s", formatKillTime(3), "15.40s")
            )
        }
        if (!M4State.inBoss()) return emptyList()

        val lines = completedSplits.sortedBy { it.bear }.map { split ->
            splitLine(
                split.bear,
                formatSeconds(split.spawnStartTime),
                formatSeconds(split.spawnTime),
                formatSeconds(split.deathTime),
                formatKillTime(split.killTime),
                formatSeconds(split.deathTime)
            )
        }.toMutableList()

        val currentBear = completedSplits.size + 1
        if (!M4State.ended && currentBear <= 4) {
            val spawnStartTime = BearTracker.bearSpawnStartTimes.getOrNull(currentBear - 1)
            val spawnTime = BearTracker.bearSpawnTimes.getOrNull(currentBear - 1)
            val killTime = spawnTime?.let { formatKillTime(M4State.timer - it) } ?: formatPendingKillTime()

            lines.add(
                splitLine(
                    currentBear,
                    formatOptionalSeconds(spawnStartTime, M4State.timer),
                    formatOptionalSeconds(spawnTime, M4State.timer),
                    formatSeconds(M4State.timer),
                    killTime,
                    formatSeconds(M4State.timer)
                )
            )
        }

        val totalTicks = if (M4State.ended) {
            BearTracker.bearKillTimes.lastOrNull() ?: M4State.timer
        } else {
            M4State.timer
        }
        lines.add(splitTotalLine(currentBear.coerceAtMost(4), "", "", "", "", formatSeconds(totalTicks)))

        return lines
    }

    private fun recordCompletedSplit() {
        val index = BearTracker.bearKillTimes.lastIndex
        if (index < 0) return

        val spawnStartTime = BearTracker.bearSpawnStartTimes.getOrNull(index) ?: 0
        val spawnTime = BearTracker.bearSpawnTimes.getOrNull(index) ?: spawnStartTime
        val deathTime = BearTracker.bearKillTimes[index]

        completedSplits.add(
            BearSplit(
                bear = index + 1,
                spawnStartTime = spawnStartTime,
                spawnTime = spawnTime,
                deathTime = deathTime,
                killTime = deathTime - spawnTime
            )
        )
    }

    private fun splitLine(bear: Int, spawnStart: String, spawn: String, death: String, kill: String, total: String): String {
        return parseSplitTemplate(splitText, bear, spawnStart, spawn, death, kill, total)
    }

    private fun splitTotalLine(bear: Int, spawnStart: String, spawn: String, death: String, kill: String, total: String): String {
        return parseSplitTemplate(splitTotalText, bear, spawnStart, spawn, death, kill, total)
    }

    private fun parseSplitTemplate(template: String, bear: Int, spawnStart: String, spawn: String, death: String, kill: String, total: String): String {
        return template
            .replace($$"$bear", bear.toString())
            .replace($$"$spawnStart", spawnStart)
            .replace($$"$spawn", spawn)
            .replace($$"$death", death)
            .replace($$"$total", total)
            .replace($$"+$kill", kill)
            .replace($$"$kill", kill)

    }

    private fun formatSeconds(ticks: Int): String {
        return "${(ticks / 20.0).toFixed(decimals)}s"
    }

    private fun formatOptionalSeconds(ticks: Int?, fallbackTicks: Int): String {
        return formatSeconds(ticks ?: fallbackTicks)
    }

    private fun formatKillTime(ticks: Int): String {
        val color = when {
            ticks > 7 -> "§c"
            ticks > 2 -> "§e"
            else -> "§a"
        }

        return "$color+${formatSeconds(ticks)}"
    }

    private fun formatPendingKillTime(): String {
        return "§a+0.0s"
    }

    private fun loadPersonalBests() {
        if (!personalBestsFile.exists()) return

        val type = object : TypeToken<MutableMap<String, Float>>() {}.type
        val loaded = runCatching {
            gson.fromJson<MutableMap<String, Float>>(personalBestsFile.readText(), type)
        }.getOrNull() ?: return

        personalBests.clear()
        personalBests.putAll(loaded)
    }

    private fun savePersonalBests() {
        if (!personalBestsFile.parentFile.exists()) {
            personalBestsFile.parentFile.mkdirs()
        }

        personalBestsFile.writeText(gson.toJson(personalBests))
    }

    private fun reportBearPersonalBest(bearIndex: Int, ticks: Int) {
        val floor = if (DungeonUtils.floor!!.isMM) "M4" else "F4"
        val key = "$floor Bear $bearIndex"
        val seconds = ticks / 20f
        val oldPb = personalBests[key] ?: 9999f

        val pbMessage = if (oldPb > seconds) {
            personalBests[key] = seconds
            savePersonalBests()
            "§7(§d§lNew PB§r§7) Old PB was §8${oldPb.toFixed(2)}s"
        } else {
            "§8(§7${oldPb.toFixed(2)}§8)s"
        }

        modMessage("§d$key §7took §b${seconds.toFixed(2)} §7$pbMessage")
    }

    private var displayedProgress = 0.0
    private var lastFrameNanos = 0L

    init {
        loadPersonalBests()

        on<M4Event.BearSpawn> {
            val bearIndex = BearTracker.bearSpawnStartTimes.size
            if (bearIndex < 1) return@on
            reportBearPersonalBest(bearIndex, M4State.timer)
        }

        on<M4Event.BearKill> {
            recordCompletedSplit()
        }

        on<LevelEvent.Load> {
            completedSplits.clear()
            loadPersonalBests()
        }

        on<RenderExtractEvent> {
            if (!experimentalRender || !M4State.inBoss()) return@on

            val predictions = ParticleTrails.predictions
            if (predictions.size < 2) return@on

            val remaining = ParticleTrails.predictionsRemaining
            if (remaining < 0) return@on

            val total = predictions.size
            val targetProgress = ((total - remaining).toDouble() / total).coerceIn(0.0, 1.0)

            val now = System.nanoTime()
            val dt = if (lastFrameNanos == 0L) 0.0
            else ((now - lastFrameNanos) / 1_000_000_000.0).coerceAtMost(0.1)
            lastFrameNanos = now

            val progress = if (interpolation) {
                val alpha = 1 - kotlin.math.exp(-12 * dt)
                displayedProgress += (targetProgress - displayedProgress) * alpha
                displayedProgress
            } else {
                displayedProgress = targetProgress
                targetProgress
            }

            val start = predictions.first().pos
            val end = predictions.last().pos
            val split = start.add(end.subtract(start).scale(progress))

            drawLine(listOf(start, split), Colors.MINECRAFT_LIGHT_PURPLE, false, 10f)
            drawLine(listOf(split, end), Colors.MINECRAFT_GRAY, false, 10f)
        }
    }
}