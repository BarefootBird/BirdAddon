package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.features.impl.m4.ExtraStats
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.modMessage
import com.github.stivais.commodore.Commodore

val extraStatsCommand = Commodore("bearstats") {

    runs { runId: String?, bear: String? ->

        val bearId = bear?.toIntOrNull()

        if (bearId == null || runId == null) {
            modMessage("bearId or runId missing")
            return@runs
        }

        ExtraStats.showBearStats(runId, bearId)
    }
}