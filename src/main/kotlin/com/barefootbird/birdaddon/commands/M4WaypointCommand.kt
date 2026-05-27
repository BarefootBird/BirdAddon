package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.features.impl.m4.Waypoints.addWaypoint
import com.barefootbird.birdaddon.features.impl.m4.Waypoints.exportWaypoints
import com.barefootbird.birdaddon.features.impl.m4.Waypoints.importWaypoints
import com.barefootbird.birdaddon.features.impl.m4.Waypoints.removeWaypoint
import com.barefootbird.birdaddon.utils.modMessage
import com.github.stivais.commodore.Commodore
import com.odtheking.odin.OdinMod.mc
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

val classMap = mapOf(
    "berserk" to "Berserk",
    "bers" to "Berserk",
    "b" to "Berserk",
    "healer" to "Healer",
    "heal" to "Healer",
    "h" to "Healer",
    "archer" to "Archer",
    "arch" to "Archer",
    "a" to "Archer",
    "mage" to "Mage",
    "m" to "Mage",
    "tank" to "Tank",
    "t" to "Tank"
)
var patternStartEnd = Regex(
    "^b\\d(spawnstart|spawn|kill)$|^(\\d+(\\.\\d+)?|\\.\\d+)s$|^bossstart$|^bossend$"
)

val waypointCommand = Commodore("m4wp") {
    literal("add").runs { clazz: String?, start: String?, end: String? ->

        if (clazz == null || start == null || end == null) {
            modMessage("Usage: /m4wp add <class> <start> <end>\n" +
                    "Options for start/end are:\n" +
                    "b<bear number>spawnstart (for when the timer starts counting down)\n" +
                    "b<bear number>spawn\n" +
                    "b<bear number>kill\n" +
                    "<number of seconds in to boss>s\n" +
                    "bossstart\n" +
                    "bossend");
            return@runs
        }

        if (!classMap.keys.contains(clazz.lowercase())) {
            modMessage("invalid class")
            return@runs
        }

        if (!start.matches(patternStartEnd) || !end.matches(patternStartEnd)) {
            modMessage("Invalid start or end value. Please use one of the following formats:\n" +
                    "b<bear number>spawnstart\n" +
                    "b<bear number>spawn\n" +
                    "b<bear number>kill\n" +
                    "<number of seconds in to boss>s\n" +
                    "bossstart\n" +
                    "bossend");
            return@runs
        }


        val player = mc.player ?: return@runs
        val hit = player.pick(100.0, 0f, false)
        if (hit.type != HitResult.Type.BLOCK) return@runs
        val pos = (hit as BlockHitResult).blockPos

        addWaypoint(
            pos,
            clazz,
            start,
            end
        )
    }

    literal("remove").runs {
        val player = mc.player ?: return@runs
        val hit = player.pick(100.0, 0f, false)
        if (hit.type != HitResult.Type.BLOCK) return@runs
        val pos = (hit as BlockHitResult).blockPos
        removeWaypoint(pos)
    }

    literal("export").runs {
        exportWaypoints()
    }

    literal("import").runs {
        importWaypoints()
    }

}