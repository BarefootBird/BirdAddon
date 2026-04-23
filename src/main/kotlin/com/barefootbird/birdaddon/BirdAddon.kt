package com.barefootbird.birdaddon

import com.barefootbird.birdaddon.commands.startWebserverCommand
import com.barefootbird.birdaddon.commands.stopWebserverCommand
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
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object BirdAddon : ClientModInitializer {

    override fun onInitializeClient() {
        println("Bird Addon initialized!")

        // Register commands by adding to the array
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(startWebserverCommand, stopWebserverCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        // Register objects to event bus by adding to the list
        listOf(this, M4State, M4Mobs).forEach { EventBus.subscribe(it) }

        // Register modules by adding to the list
        ModuleManager.registerModules(ModuleConfig("BirdAddon.json"), SpiritBearTimer, M4Highlight,
            WishyWishy, M4Waypoints, M4Timer, M4Logging, ThornStunTimer, OverkillDisplay)
    }
}
