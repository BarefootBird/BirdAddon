package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.ChatManager.hideMessage

object HideMessages: Module(
    name = "Hide Messages",
    description = "Hides some messages in m4",
    category = Category.M4
) {
    private val crowd by BooleanSetting("Crowd", true, desc = "Hides the messages from crowd")
    private val thorn by BooleanSetting("Thorn", true, desc = "Hides the messages from thorn")
    private val exploSheep by BooleanSetting("Sheep Explosion", true, desc = "Hides the messages from sheep explosions")
    private val chickenMine by BooleanSetting("Chicken Mines", true, desc = "Hides the messages from chicken mines")
    private val chickenLightning by BooleanSetting("Chicken Lightning", true, desc = "Hides the messages from chicken lightning")
    private val bearSpawn by BooleanSetting("Bear Spawn", true, desc = "Hides the messages from bear spawns")
    private val bowDrop by BooleanSetting("Bow Drop", true, desc = "Hides the messages from bow drops")
    private val bowPickup by BooleanSetting("Bow Pickup", true, desc = "Hides the messages from bow pickups")
    private val bowShot by BooleanSetting("Bow Shot", true, desc = "Hides the messages from bow shots")


    private val chickenRegex = Regex("""^A Chicken Mine exploded, hitting you for [\d,]+(?:\.\d+)? damage\.$""")
    private val crowdRegex = Regex("""^\[CROWD] [^:]+: (.+)$""")
    private val thornRegex = Regex("""^\[BOSS] Thorn: .+$""")
    private val bearSpawnRegex = Regex("^A Spirit Bear has appeared!$")
    private val bowDropRegex = Regex("^The Spirit Bow has dropped!$")
    private val bowPickupRegex = Regex("""^You picked up the Spirit Bow! Use it to attack Thorn!$""")
    private val chickenLightningRegex = Regex("""^The Spirit Chicken's lightning struck you for [\d,]+(?:\.\d+)? damage\.$""")
    private val exploSheepRegex = Regex("""^A Spirit Sheep exploded, hitting you for [\d,]+(?:\.\d+)? damage\.$""")
    private val bowShotRegex = Regex("""^The Spirit Bow disintegrates as you fire off the shot!$""")


    init {

        on<ChatPacketEvent> {
            if (!M4State.inBoss()) return@on

            when {
                chickenMine && chickenRegex.matches(value) -> hideMessage()
                crowd && crowdRegex.matches(value) -> hideMessage()
                thorn && thornRegex.matches(value) -> hideMessage()
                bearSpawn && bearSpawnRegex.matches(value) -> hideMessage()
                bowDrop && bowDropRegex.matches(value) -> hideMessage()
                bowPickup && bowPickupRegex.matches(value) -> hideMessage()
                chickenLightning && chickenLightningRegex.matches(value) -> hideMessage()
                exploSheep && exploSheepRegex.matches(value) -> hideMessage()
                bowShot && bowShotRegex.matches(value) -> hideMessage()
            }
        }
    }
}