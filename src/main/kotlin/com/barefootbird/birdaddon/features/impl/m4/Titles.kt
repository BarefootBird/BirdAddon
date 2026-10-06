package com.barefootbird.birdaddon.features.impl.m4


import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.barefootbird.birdaddon.utils.debugMessage
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.MessageEvent.Chat
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.network.chat.Component

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
    private val bowDisintegrateWarning by StringSetting("Bow Disintegrate warning", "§0SHOOT §4THE §5BOW", desc = "Shows a title when the bow is close to disintegrating")

    private val bearTimerStarted by StringSetting("Bear Timer Started", "§cSTOP KILLING", desc = "Shows a title when timer starts")
    private val bearSpawned by StringSetting("Bear Spawned", "§5Bear Spawned", desc = "Shows a title when bear spawns")
    private val bearKilled by StringSetting("Bear Killed", "§aResume Killing", desc = "Shows a title when bear dies")

    val bowMiss = Regex("""^\[CROWD] [^:]+: (Yeah!!! Keep dodging them Thorn!|[A-Za-z0-9_]+ missed the shot! No way!! Hahaha|My goodness, [A-Za-z0-9_]+ really can't aim!!|Alright those humans are a joke, missing easy shots like that\.\.\.|[A-Za-z0-9_]+ has no thumbs!)$""")
    val bowPickup = "You picked up the Spirit Bow! Use it to attack Thorn!"


    fun setTitle(title: String) {
        mc.gui.setTimes(0, titleDuration, 5)
        mc.gui.setTitle(Component.literal(title))
    }

    private var pickupTime = -10000

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

            // bow disintegrates after 400t so 280t means the player has 6s from the title appearing to shoot which should be plenty
            if (M4State.timer - pickupTime > 280 && mc.player?.inventory?.contains { debugMessage(it.displayName.string); it.displayName.string.contains("Spirit Bow")} == true) {
                setTitle(bowDisintegrateWarning)
            }
        }

        on<LevelEvent.Load> {
            pickupTime = -10000
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

        on<Chat> {
            if (!M4State.inBoss()) return@on

            if (message == bowPickup) {
                pickupTime = M4State.timer
                if (pickupWarning != "" && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.TANK) {
                    setTitle(pickupWarning)
                }

                if (tankPickup != "" && DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.TANK) {
                    setTitle(pickupWarning)
                }
            }

            if (bowMiss.matches(message) && missWarning != "") {
                setTitle(missWarning)
            }
        }
    }
}
