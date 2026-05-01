package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.features.impl.m4.Replay
import com.github.stivais.commodore.Commodore

val replayCommand = Commodore("m4rp") {
    literal("play").runs {
        Replay.play()
    }
    literal("pause").runs {
        Replay.pause()
    }
    literal("step").runs { by: String? ->
        Replay.step()
    }
    literal("speed").runs { speed: String ->
        Replay.setPlaySpeed(speed)
    }
    literal("goto").runs { time: String ->
        Replay.seek(time)
    }
    literal("load").runs { fileName: String? ->
        if (fileName != null && fileName != "") {
            Replay.loadReplay(fileName)
        } else {
            Replay.openLoaderScreen()
        }
    }
}