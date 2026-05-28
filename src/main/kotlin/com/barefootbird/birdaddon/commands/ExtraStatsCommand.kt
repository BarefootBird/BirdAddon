package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.features.impl.m4.ExtraStats
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.github.stivais.commodore.Commodore

val extraStatsCommand = Commodore("bearstats") {

    runs { bear: String? ->

        val bearId = bear?.toIntOrNull()

        if (bearId == null || bearId !in 1..M4State.bearKillTimes.size) {
            modMessage("Invalid bear number")
            return@runs
        }

        ExtraStats.showBearStats(bearId)
    }
}