package com.barefootbird.birdaddon.features.impl.m4


import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket

object Titles: Module(
    name = "Titles",
    description = "Custom Titles for m4 (leave them blank to disable them)",
    category = Category.M4
) {
    private val hideDefault by BooleanSetting("Hide Default Titles", true, desc = "Hides the titles for picking up bows and bears dying")
    private val titleDuration by NumberSetting("Title Duration Ticks", 20, 1,  200, 1, "How long to display the title for")

    private val missWarning by StringSetting("Miss Warning", "§cBow Missed", desc = "Shows a title when bow is missed")
    private val pickupWarning by StringSetting("Non-Tank Pickup Warning", "§cBow Picked Up", desc = "Shows a title when bow is picked up if you're not on tank")
    private val tankPickup by StringSetting("Tank Bow Pickup", "§aBow Picked Up", desc = "Shows a title when bow is picked up if you're on tank")

    private val bearTimerStarted by StringSetting("Bear Timer Started", "§cSTOP KILLING", desc = "Shows a title when timer starts")
    private val bearSpawned by StringSetting("Bear Spawned", "§5Bear Spawned", desc = "Shows a title when bear spawns")
    private val bearKilled by StringSetting("Bear Killed", "§aResume Killing", desc = "Shows a title when bear dies")

    private val wishTitle by StringSetting("Wish Title", "§dWISH WISH WISH", desc = "Shows a title when it's time to wish")
    private val wishThreshold by NumberSetting("Wish Threshold", 100000, 50000, 200000, 10000, "Tank's hp needs to be bigger than this number to show wish title")

    val bowMiss = Regex("""^\[CROWD] [^:]+: (Yeah!!! Keep dodging them Thorn!|[A-Za-z0-9_]+ missed the shot! No way!! Hahaha|My goodness, [A-Za-z0-9_]+ really can't aim!!|Alright those humans are a joke, missing easy shots like that\.\.\.|[A-Za-z0-9_]+ has no thumbs!)$""")
    val bowPickup = "You picked up the Spirit Bow! Use it to attack Thorn!"

    val wishRegex = Regex("""^Your Wish healed your entire team for [\d,.]+ health and shielded them for [\d,.]+!$""")

    private var tankInMastiff = false
    private var wished = false

    fun setTitle(title: String) {
        mc.gui.setTimes(0, titleDuration, 5)
        mc.gui.setTitle(Component.literal(title))
    }

    init {
        on<M4Event.BearSpawnStart> {
            setTitle(bearTimerStarted)
        }
        on<M4Event.BearSpawn> {
            setTitle(bearSpawned)
        }
        on<M4Event.BearKill> {
            setTitle(bearKilled)
        }

        on<TickEvent.Server> {
            if (tankInMastiff && !wished && wishTitle != "") {
                setTitle(wishTitle)
            }
        }

        on<LevelEvent.Load> {
            wished = false
            tankInMastiff = false
        }

        onReceive<ClientboundSetSubtitleTextPacket> {
            if (!M4State.inBoss() || !hideDefault) return@onReceive
            it.cancel()
        }
        onReceive<ClientboundSetTitlesAnimationPacket> {
            if (!M4State.inBoss() || !hideDefault) return@onReceive
            it.cancel()
        }
        onReceive<ClientboundSetTitleTextPacket> {
            if (!M4State.inBoss() || !hideDefault) return@onReceive
            it.cancel()
        }

        on<ChatPacketEvent> {
            if (!M4State.inBoss()) return@on

            if (value == bowPickup && pickupWarning != "" && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.TANK) {
                setTitle(pickupWarning)
            }

            if (value == bowPickup && tankPickup != "" && DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.TANK) {
                setTitle(pickupWarning)
            }

            if (bowMiss.matches(value) && missWarning != "") {
                setTitle(missWarning)
            }
            if (wishRegex.matches(value)) wished = true
        }

        onReceive<ClientboundSetPlayerTeamPacket> { event ->
            if (!M4State.inBoss() || DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.HEALER) return@onReceive
            val packet = event.packet
            if (packet is ClientboundSetPlayerTeamPacket) {

                // Get the message of the scoreboard thats being updated (code yoinked from devonian)
                val opt = packet.parameters
                if (!opt.isPresent) return@onReceive
                val team = opt.get()
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return@onReceive
                val message = "${teamPrefix}${teamSuffix.trim()}".noControlCodes

                // Use it to check tank's hp
                if (!message.contains("[T]")) return@onReceive
                val health = Regex("""^\[T]\s+\S+\s+([\d,]+(?:\.\d+)?)""")
                    .find(message)
                    ?.groupValues
                    ?.get(1)
                    ?.replace(",", "")
                    ?.toIntOrNull()
                    ?: return@onReceive

                tankInMastiff = health >= wishThreshold
            }
        }
    }
}