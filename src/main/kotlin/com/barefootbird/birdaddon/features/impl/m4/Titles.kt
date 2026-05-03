package com.barefootbird.birdaddon.features.impl.m4


import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.setTitle
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket

object Titles: Module(
    name = "Titles",
    description = "Custom Titles for m4 (leave them blank to disable them)",
    category = Category.M4
) {
    private val hideDefault by BooleanSetting("Hide Default Titles", true, desc = "Hides the titles for picking up bows and bears dying")

    private val missWarning by StringSetting("Miss Warning", "§cBow Missed", desc = "Shows a title when bow is missed")
    private val pickupWarning by StringSetting("Non-Tank Pickup Warning", "§cBow Picked Up", desc = "Shows a title when bow is picked up if you're not on tank")
    private val tankPickup by StringSetting("Tank Bow Pickup", "§aBow Picked Up", desc = "Shows a title when bow is picked up if you're on tank")

    private val bearTimerStarted by StringSetting("Bear Timer Started", "§cSTOP KILLING", desc = "Shows a title when timer starts")
    private val bearSpawned by StringSetting("Bear Spawned", "§5Bear Spawned", desc = "Shows a title when bear spawns")
    private val bearKilled by StringSetting("Bear Killed", "§aResume Killing", desc = "Shows a title when bear dies")

    val bowMiss = Regex("""^\[CROWD] [^:]+: (Yeah!!! Keep dodging them Thorn!|[A-Za-z0-9_]+ missed the shot! No way!! Hahaha|My goodness, [A-Za-z0-9_]+ really can't aim!!|Alright those humans are a joke, missing easy shots like that\.\.\.|[A-Za-z0-9_]+ has no thumbs!)$""")
    val bowPickup = "You picked up the Spirit Bow! Use it to attack Thorn!"



    fun handleBearSpawnStart() {
        if (enabled) {
            setTitle(bearTimerStarted)
        }
    }
    fun handleBearSpawn() {
        if (enabled) {
            setTitle(bearSpawned)
        }
    }
    fun handleBearKill() {
        if (enabled) {
            setTitle(bearKilled)
        }
    }

    init {
        onReceive<ClientboundSetSubtitleTextPacket> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@onReceive
            if (!hideDefault) return@onReceive
            it.cancel()
        }
        onReceive<ClientboundSetTitlesAnimationPacket> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@onReceive
            if (!hideDefault) return@onReceive
            it.cancel()
        }
        onReceive<ClientboundSetTitleTextPacket> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@onReceive
            if (!hideDefault) return@onReceive
            it.cancel()
        }

        on<ChatPacketEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            if (value == bowPickup && pickupWarning != "" && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.Tank) {
                setTitle(pickupWarning)
            }

            if (value == bowPickup && tankPickup != "" && DungeonUtils.currentDungeonPlayer.clazz == DungeonClass.Tank) {
                setTitle(pickupWarning)
            }

            if (bowMiss.matches(value) && missWarning !="") {
                setTitle(missWarning)
            }
        }
    }
}