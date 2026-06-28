package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.modMessage
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
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import java.io.File

object SpiritBear: Module(
    name = "Spirit Bear",
    description = "Shows the state of the spirit bear spawns",
    category = Category.M4
) {
    private val killPhaseText by StringSetting(
        "Kill Phase Text",
        $$"§6Bear $bear: §a$kills/$cap",
        desc = $$"HUD format for when bears are being killed"
    )

    private val spawningText by StringSetting(
        "Spawning Text",
        $$"§6Bear $bear: §c$timer",
        desc = $$"HUD format for when bear is about to spawn"
    )

    private val spawnedText by StringSetting(
        "Spawned Text",
        $$"§6Bear $bear: §4!",
        desc = $$"HUD format for when bear has spawned"
    )

    private val splitText by StringSetting(
        "Split Text",
        $$"§6Bear $bear: §b$death Kill: $kill",
        96,
        desc = $$"HUD format for each bear split. Options: $bear, $spawnStart, $spawn, $death, $kill, $total"
    )

    private val splitTotalText by StringSetting(
        "Split Total Text",
        $$"§7Total: §b$total",
        96,
        desc = $$"HUD format for the total boss time at the end of the splits. Options: $bear, $spawnStart, $spawn, $death, $kill, $total"
    )

    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", false) { example ->
        textDim(timerText(example), 0, 0, Colors.WHITE)
    }

    private val splitsHud by HUD("Spirit Bear Splits", "Displays Spirit Bear split times in the HUD.", false) { example ->
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

    private val showTicks by BooleanSetting("Show Timer in Ticks", true, desc = "Changes the timer to be in ticks instead of seconds")

    private val decimals by NumberSetting("Decimals", 2, 1, 2, 1, "How many decimals to show")

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
            M4State.bearTimer > 0 -> spawningText
            M4State.bearTimer == 0 -> spawnedText
            else -> killPhaseText
        }

        val bear = M4State.bearKillTimes.size + 1
        val kills = M4State.kills
        val cap = M4State.maxKills

        val timerValue = when {
            M4State.bearTimer > 0 -> {
                val ticksLeft = M4State.bearTimer.toDouble()
                if (showTicks) {
                    "§c${ticksLeft.toFixed(0).padStart(2, '0')}"
                } else {
                    "§c${(ticksLeft / 20.0).toFixed(decimals)}s"
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
            val spawnStartTime = M4State.bearSpawnStartTimes.getOrNull(currentBear - 1)
            val spawnTime = M4State.bearSpawnTimes.getOrNull(currentBear - 1)
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
            M4State.bearKillTimes.lastOrNull() ?: M4State.timer
        } else {
            M4State.timer
        }
        lines.add(splitTotalLine(currentBear.coerceAtMost(4), "", "", "", "", formatSeconds(totalTicks)))

        return lines
    }

    private fun recordCompletedSplit() {
        val index = M4State.bearKillTimes.lastIndex
        if (index < 0) return

        val spawnStartTime = M4State.bearSpawnStartTimes.getOrNull(index) ?: 0
        val spawnTime = M4State.bearSpawnTimes.getOrNull(index) ?: spawnStartTime
        val deathTime = M4State.bearKillTimes[index]

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
        val key = "Bear $bearIndex"
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

    init {
        loadPersonalBests()

        on<M4Event.BearSpawnStart> {
            val bearIndex = M4State.bearSpawnStartTimes.size
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
    }
}
