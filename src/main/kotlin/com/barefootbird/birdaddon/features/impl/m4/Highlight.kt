package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.Chicken
import com.barefootbird.birdaddon.utils.Cow
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
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
import com.barefootbird.birdaddon.utils.Rabbit
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf

import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.phys.AABB


object Highlight: Module(
    name = "Highlight",
    description = "Highlights M4 animals",
    category = Category.M4
) {
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    private val hideNameTags by BooleanSetting("Hide Name tags", true, desc = "Hides animal name tags")
    private val hideMobs by BooleanSetting("Hide highlighted mobs", false, desc = "Hides mobs")

    private val highlightThorn by BooleanSetting("Thorn Highlight", true, desc = "Highlights thorn")

    private val highlightBear by BooleanSetting("Bear Highlight", true, desc = "Highlights bears")
    private val onlyShowBearOnMage by BooleanSetting("Only Show Bear On Mage", false, desc = "Only shows the bear on mage class").withDependency { highlightBear }
    private val noInterpolateBear by BooleanSetting("No Bear Interpolation", true, desc = "Removes interpolation from bears").withDependency { highlightBear }
    val bearColor by ColorSetting("Bear Color", Colors.MINECRAFT_DARK_PURPLE, true, desc = "Color of bear highlight").withDependency { highlightBear }

    private val highlightWolves by BooleanSetting("Wolf Highlight", true, desc = "Highlights wolves")
    private val highlightBats by BooleanSetting("Bat Highlight", true, desc = "Highlights bats")
    private val highlightSheep by BooleanSetting("Sheep Highlight", true, desc = "Highlights sheep")
    private val highlightCow by BooleanSetting("Cow Highlight", true, desc = "Highlights cows")
    private val highlightChicken by BooleanSetting("Chicken Highlight", true, desc = "Highlights chickens")
    private val highlightRabbit by BooleanSetting("Rabbit Highlight", true, desc = "Highlights rabbits")

    val thornColor by ColorSetting("Thorn Color", Colors.WHITE, true, desc = "Color of thorn highlight").withDependency { highlightThorn }
    val wolfColor by ColorSetting("Wolf Color", Colors.MINECRAFT_GOLD, true, desc = "Color of wolf highlight").withDependency { highlightWolves }
    val batColor by ColorSetting("Bat Color", Colors.MINECRAFT_DARK_BLUE, true, desc = "Color of bat highlight").withDependency { highlightBats }
    val sheepColor by ColorSetting("Sheep Color", Colors.MINECRAFT_YELLOW, true, desc = "Color of sheep highlight").withDependency { highlightSheep }
    val cowColor by ColorSetting("Cow Color", Colors.MINECRAFT_AQUA, true, desc = "Color of cow highlight").withDependency { highlightCow }
    val chickenColor by ColorSetting("Chicken Color", Colors.MINECRAFT_RED, true, desc = "Color of chicken highlight").withDependency { highlightChicken }
    val rabbitColor by ColorSetting("Rabbit Color", Colors.MINECRAFT_DARK_GREEN, true, desc = "Color of rabbit highlight").withDependency { highlightRabbit }


    private val searchBox = AABB(-36.0, -36.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    @JvmStatic
    fun shouldHideEntity(entity: Entity): Boolean {
        if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return false
        if (hideNameTags) {
            if (entity is ArmorStand && entity.name.string.contains("❤")) return true
        }
        if (hideMobs) {
            if (entity is Bat && highlightBats) return true
            if (entity is Chicken && highlightChicken) return true
            if (entity is Rabbit && highlightRabbit) return true
            if (entity is Sheep && highlightSheep) return true
            if (entity is Cow && highlightCow) return true
            if (entity is Wolf && highlightWolves) return true
            if (entity is Ghast && highlightThorn) return true
        }
        return false
    }

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
            runCatching {
                val style = renderStyle

                val entities = mutableListOf<Pair<List<net.minecraft.world.entity.Entity>, Color>>()


                if (highlightSheep) entities.add(sheep.toList() to sheepColor)
                if (highlightWolves) entities.add(wolves.toList() to wolfColor)
                if (highlightBats) entities.add(bats.toList() to batColor)
                if (highlightChicken) entities.add(chickens.toList() to chickenColor)
                if (highlightRabbit) entities.add(rabbits.toList() to rabbitColor)
                if (highlightCow) entities.add(cows.toList() to cowColor)
                if (highlightThorn) entities.add(ghasts.toList() to thornColor)

                entities.forEach { (entities, color) ->
                    entities.toList().forEach { entity ->
                        drawStyledBox(entity.renderBoundingBox, color, style, true)
                    }
                }

                if (highlightBear && !(onlyShowBearOnMage && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.Mage)) {
                    bears.forEach { bear ->
                        if (noInterpolateBear) {
                            drawStyledBox(bear.boundingBox, bearColor, style, true)
                        } else {
                            drawStyledBox(bear.renderBoundingBox, bearColor, style, true)
                        }
                    }
                }
            }
        }
    }
}