package com.barefootbird.birdaddon

import com.barefootbird.birdaddon.commands.debugCommand
import com.barefootbird.birdaddon.commands.extraStatsCommand
import com.barefootbird.birdaddon.commands.mainCommand
import com.barefootbird.birdaddon.commands.waypointCommand
import com.barefootbird.birdaddon.events.EventDispatcher
import com.barefootbird.birdaddon.features.impl.m4.Decoy
import com.barefootbird.birdaddon.features.impl.m4.ExtraStats
import com.barefootbird.birdaddon.features.impl.m4.HideMessages
import com.barefootbird.birdaddon.features.impl.m4.Titles
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.barefootbird.birdaddon.features.impl.m4.Highlight
import com.barefootbird.birdaddon.features.impl.m4.MobCounters
import com.barefootbird.birdaddon.features.impl.m4.OverkillDisplay
import com.barefootbird.birdaddon.features.impl.m4.RabbitCountdown
import com.barefootbird.birdaddon.features.impl.m4.Timer
import com.barefootbird.birdaddon.features.impl.m4.RenderOptimizer
import com.barefootbird.birdaddon.features.impl.m4.Sounds
import com.barefootbird.birdaddon.features.impl.m4.SpiritBear
import com.barefootbird.birdaddon.features.impl.m4.Tac
import com.barefootbird.birdaddon.features.impl.m4.ThornStunTimer
import com.barefootbird.birdaddon.features.impl.m4.Trajectories
import com.barefootbird.birdaddon.features.impl.m4.Waypoints
import com.barefootbird.birdaddon.utils.Islands
import com.barefootbird.birdaddon.utils.M4Mobs
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.ParticleTrails
import com.odtheking.odin.OdinMod.mc
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import org.apache.logging.log4j.LogManager
import java.io.File

object BirdAddon : ClientModInitializer {
    val logger = LogManager.getLogger(BirdAddon::class.java.name)


    override fun onInitializeClient() {
        logger.info("BirdAddon initialized")

        // Register commands by adding to the array
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(waypointCommand, extraStatsCommand, debugCommand, mainCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        // Register objects to event bus by adding to the list
        listOf(this, M4State, M4Mobs, Islands, EventDispatcher, ParticleTrails).forEach { EventBus.subscribe(it) }

        // Register modules by adding to the list
        ModuleManager.registerModules(ModuleConfig("BirdAddon.json"),
            SpiritBear, Highlight, Waypoints, Timer, ThornStunTimer, OverkillDisplay,
            Titles, MobCounters, Decoy, Tac, HideMessages, ExtraStats, Sounds, RenderOptimizer, Trajectories,
            RabbitCountdown
        )
    }
}
