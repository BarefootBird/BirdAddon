package com.barefootbird.birdaddon.features.impl.dungeon

import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.barefootbird.birdaddon.utils.M4Mobs.bats
import com.barefootbird.birdaddon.utils.M4Mobs.chickens
import com.barefootbird.birdaddon.utils.M4Mobs.cows
import com.barefootbird.birdaddon.utils.M4Mobs.ghasts
import com.barefootbird.birdaddon.utils.M4Mobs.rabbits
import com.barefootbird.birdaddon.utils.M4Mobs.sheep
import com.barefootbird.birdaddon.utils.M4Mobs.wolves
import com.barefootbird.birdaddon.utils.M4Mobs.bears
import com.odtheking.odin.features.Category

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB


object M4Highlight: Module(
    name = "M4 Highlight",
    description = "Highlights M4 animals",
    category = Category.BOSS
) {
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val depth by BooleanSetting("Depth", true, desc = "no show through da wall")
    private val highlightThorn by BooleanSetting("Thorn Highlight", true, desc = "Highlights thorn")
    private val highlightBear by BooleanSetting("Bear Highlight", true, desc = "Highlights bears")
    private val noInterpolateBear by BooleanSetting("No Bear Interpolation", true, desc = "Removes interpolation from bears").withDependency { highlightBear }

    private val hideNameTags by BooleanSetting("Hide Name tags", true, desc = "Hides animal name tags")
    private val thornColor by ColorSetting("Thorn Color", Colors.WHITE, true, desc = "Color of thorn highlight").withDependency { highlightThorn }
    private val wolfColor by ColorSetting("Wolf Color", Colors.MINECRAFT_GOLD, true, desc = "Color of wolf highlight")
    private val batColor by ColorSetting("Bat Color", Colors.MINECRAFT_DARK_BLUE, true, desc = "Color of bat highlight")
    private val sheepColor by ColorSetting("Sheep Color", Colors.MINECRAFT_YELLOW, true, desc = "Color of sheep highlight")
    private val cowColor by ColorSetting("Cow Color", Colors.MINECRAFT_AQUA, true, desc = "Color of cow highlight")
    private val chickenColor by ColorSetting("Chicken Color", Colors.MINECRAFT_RED, true, desc = "Color of chicken highlight")
    private val rabbitColor by ColorSetting("Rabbit Color", Colors.MINECRAFT_DARK_GREEN, true, desc = "Color of rabbit highlight")
    private val bearColor by ColorSetting("Bear Color", Colors.MINECRAFT_DARK_PURPLE, true, desc = "Color of bear highlight")


    private val searchBox = AABB(-36.0, -36.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    init {

        on<TickEvent.Server> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            if (hideNameTags) {
                runCatching {
                    val allEntities = OdinMod.mc.level?.getEntities(null, searchBox)?.toList() ?: emptyList()
                    allEntities.filterIsInstance<ArmorStand>().forEach {
                        if (it.name.string.contains("❤")) {
                            it.isInvisible = true
                        }
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on
            val style = renderStyle

            listOf(
                sheep to sheepColor,
                wolves to wolfColor,
                bats to batColor,
                chickens to chickenColor,
                rabbits to rabbitColor,
                cows to cowColor,
                ghasts to thornColor,
            ).forEach { (entities, color) ->
                runCatching {
                    entities.toList().forEach { entity ->
                        drawStyledBox(entity.renderBoundingBox, color, style, depth)
                    }
                }
            }
            if (highlightBear) {
                bears.forEach { bear ->
                    if (noInterpolateBear) {
                        drawStyledBox(bear.boundingBox, bearColor, style, depth)

                    } else {
                        drawStyledBox(bear.renderBoundingBox, bearColor, style, depth)
                    }
                }
            }
        }

    }
}