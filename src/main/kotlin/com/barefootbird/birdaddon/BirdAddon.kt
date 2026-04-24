package com.barefootbird.birdaddon

import com.barefootbird.birdaddon.commands.startWebserverCommand
import com.barefootbird.birdaddon.commands.stopWebserverCommand
import com.barefootbird.birdaddon.commands.waypointCommand
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.barefootbird.birdaddon.features.impl.dungeon.M4Highlight
import com.barefootbird.birdaddon.features.impl.dungeon.M4Logging
import com.barefootbird.birdaddon.features.impl.dungeon.M4Timer
import com.barefootbird.birdaddon.features.impl.dungeon.M4Waypoints
import com.barefootbird.birdaddon.features.impl.dungeon.OverkillDisplay
import com.barefootbird.birdaddon.features.impl.dungeon.SpiritBearTimer
import com.barefootbird.birdaddon.features.impl.dungeon.ThornStunTimer
import com.barefootbird.birdaddon.features.impl.dungeon.WishyWishy
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.Webserver
import com.odtheking.odin.OdinMod.mc
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import org.apache.logging.log4j.LogManager
import java.io.File

object BirdAddon : ClientModInitializer {
    val logger = LogManager.getLogger(BirdAddon::class.java.name)

    fun migrateLogs() {
        val logsFolder = File(mc.gameDirectory, "m4logs/logs")
        val oldLogsFolder = File(mc.gameDirectory, "m4logs")

        if (!logsFolder.exists()) logsFolder.mkdirs()

        val oldFiles = oldLogsFolder.listFiles { f ->
            f.isFile && f.extension == "csv"
        } ?: emptyArray()

        for (file in oldFiles) {
            logger.info("BirdAddon migrating logs")
            val target = File(logsFolder, file.name)
            if (!target.exists()) {
                file.renameTo(target)
            }
        }
    }

    override fun onInitializeClient() {
        logger.info("BirdAddon initialized")
        migrateLogs()

        // Register commands by adding to the array
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(startWebserverCommand, stopWebserverCommand, waypointCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        // Register objects to event bus by adding to the list
        listOf(this, M4State, M4Mobs, Webserver).forEach { EventBus.subscribe(it) }

        // Register modules by adding to the list
        ModuleManager.registerModules(ModuleConfig("BirdAddon.json"), SpiritBearTimer, M4Highlight,
            WishyWishy, M4Waypoints, M4Timer, M4Logging, ThornStunTimer, OverkillDisplay)
    }
}
