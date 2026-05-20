package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.features.impl.m4.Replay
import com.barefootbird.birdaddon.utils.Islands.onCgm4
import com.barefootbird.birdaddon.utils.Islands.onM4Miku
import com.barefootbird.birdaddon.utils.modMessage
import com.github.stivais.commodore.Commodore

val replayCommand = Commodore("m4rp") {
    literal("play").runs {
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        Replay.play()
    }
    literal("pause").runs {
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        Replay.pause()
    }
    literal("step").runs { by: String? ->
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        Replay.step()
    }
    literal("speed").runs { speed: String ->
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        Replay.setPlaySpeed(speed)
    }
    literal("goto").runs { time: String ->
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        Replay.seek(time)
    }
    literal("load").runs { fileName: String? ->
        if (!onCgm4 && !onM4Miku) {
            modMessage("Must be on catgirlm4/m4miku's island")
            return@runs
        }
        if (fileName != null && fileName != "") {
            Replay.loadReplay(fileName)
        } else {
            Replay.openLoaderScreen()
        }
    }
}