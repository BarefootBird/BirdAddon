package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
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
import com.barefootbird.birdaddon.utils.M4Mobs.thorn
import com.barefootbird.birdaddon.utils.M4Mobs.rabbits
import com.barefootbird.birdaddon.utils.M4Mobs.sheep
import com.barefootbird.birdaddon.utils.M4Mobs.wolves
import com.barefootbird.birdaddon.utils.M4Mobs.bear
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf

import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


object Highlight: Module(
    name = "Highlight",
    description = "Highlights M4 animals",
    category = Category.M4
) {
    private val renderStyle by SelectorSetting(
        "Render Style",
        "Outline",
        listOf("Filled", "Outline", "Filled Outline"),
        desc = "Style of the box."
    )

    private val hideNameTags by BooleanSetting("Hide Name tags", true, desc = "Hides animal name tags")
    private val hideMobs by BooleanSetting("Hide highlighted mobs", false, desc = "Hides mobs")

    private val highlightThorn by BooleanSetting("Thorn Highlight", true, desc = "Highlights thorn")
    private val transThorn by BooleanSetting("Trans Thorn", true, desc = "Makes thorn trans")
    private val thornDmgFlash by BooleanSetting(
        "Thorn Damage Flash",
        true,
        desc = "Makes thorn change color when damaged"
    ).withDependency { highlightThorn }
    private val dmgFlashDuration by NumberSetting(
        "Damage duration ticks",
        10,
        1,
        80,
        1,
        "How long thorn changes color when damaged in ticks"
    ).withDependency { thornDmgFlash }

    private val highlightBear by BooleanSetting("Bear Highlight", true, desc = "Highlights bears")
    private val onlyShowBearOnMage by BooleanSetting(
        "Only Show Bear On Mage",
        false,
        desc = "Only shows the bear on mage class"
    ).withDependency { highlightBear }
    private val noInterpolateBear by BooleanSetting(
        "No Bear Interpolation",
        true,
        desc = "Removes interpolation from bears"
    ).withDependency { highlightBear }
    val bearColor by ColorSetting(
        "Bear Color",
        Colors.MINECRAFT_DARK_PURPLE,
        true,
        desc = "Color of bear highlight"
    ).withDependency { highlightBear }

    private val highlightWolves by BooleanSetting("Wolf Highlight", true, desc = "Highlights wolves")
    private val highlightBats by BooleanSetting("Bat Highlight", true, desc = "Highlights bats")
    private val highlightSheep by BooleanSetting("Sheep Highlight", true, desc = "Highlights sheep")
    private val highlightCow by BooleanSetting("Cow Highlight", true, desc = "Highlights cows")
    private val highlightChicken by BooleanSetting("Chicken Highlight", true, desc = "Highlights chickens")
    private val highlightRabbit by BooleanSetting("Rabbit Highlight", true, desc = "Highlights rabbits")

    val thornColor by ColorSetting(
        "Thorn Color",
        Colors.WHITE,
        true,
        desc = "Color of thorn highlight"
    ).withDependency { highlightThorn }
    val thornDamagedColor by ColorSetting(
        "Thorn Damaged Color",
        Colors.MINECRAFT_RED,
        true,
        desc = "Color of thorn highlight when thorn is damaged"
    ).withDependency { thornDmgFlash }

    val wolfColor by ColorSetting(
        "Wolf Color",
        Colors.MINECRAFT_GOLD,
        true,
        desc = "Color of wolf highlight"
    ).withDependency { highlightWolves }
    val batColor by ColorSetting(
        "Bat Color",
        Colors.MINECRAFT_DARK_BLUE,
        true,
        desc = "Color of bat highlight"
    ).withDependency { highlightBats }
    val sheepColor by ColorSetting(
        "Sheep Color",
        Colors.MINECRAFT_YELLOW,
        true,
        desc = "Color of sheep highlight"
    ).withDependency { highlightSheep }
    val cowColor by ColorSetting(
        "Cow Color",
        Colors.MINECRAFT_AQUA,
        true,
        desc = "Color of cow highlight"
    ).withDependency { highlightCow }
    val chickenColor by ColorSetting(
        "Chicken Color",
        Colors.MINECRAFT_RED,
        true,
        desc = "Color of chicken highlight"
    ).withDependency { highlightChicken }
    val rabbitColor by ColorSetting(
        "Rabbit Color",
        Colors.MINECRAFT_DARK_GREEN,
        true,
        desc = "Color of rabbit highlight"
    ).withDependency { highlightRabbit }


    private val searchBox = AABB(-36.0, -36.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    @JvmStatic
    fun shouldHideEntity(entity: Entity): Boolean {
        if (!enabled) return false
        if (!M4State.inBoss()) return false
        if (hideNameTags) {
            if (entity is ArmorStand && entity.name.string.contains("❤")) return true
        }
        if (highlightThorn && transThorn && entity == thorn) return true
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

    private var damaged = 0

    fun RenderEvent.Extract.drawTransBox(
        bb: AABB,
        style: Int,
        depth: Boolean,
    ) {
        val minX = bb.minX
        val minY = bb.minY
        val minZ = bb.minZ

        val maxX = bb.maxX
        val maxY = bb.maxY
        val maxZ = bb.maxZ

        val height = maxY - minY
        val stripeH = height / 5.0

        val colors = listOf(
            Color(91, 206, 250),
            Color(245, 169, 184),
            Colors.WHITE,
            Color(245, 169, 184),
            Color(91, 206, 250)
        )

        if (style == 0 || style == 2) {
            for (i in 0 until 5) {
                val y0 = minY + stripeH * i
                val y1 = minY + stripeH * (i + 1)

                val stripeBox = AABB(
                    minX, y0, minZ,
                    maxX, y1, maxZ
                )

                drawStyledBox(stripeBox, colors[i], style, depth)
            }
            return
        }


        val p000 = Vec3(minX, minY, minZ)
        val p001 = Vec3(minX, minY, maxZ)
        val p010 = Vec3(minX, maxY, minZ)
        val p011 = Vec3(minX, maxY, maxZ)

        val p100 = Vec3(maxX, minY, minZ)
        val p101 = Vec3(maxX, minY, maxZ)
        val p110 = Vec3(maxX, maxY, minZ)
        val p111 = Vec3(maxX, maxY, maxZ)

        val edges = listOf(
            p000 to p001,
            p001 to p011,
            p011 to p010,
            p010 to p000,

            p100 to p101,
            p101 to p111,
            p111 to p110,
            p110 to p100,

            p000 to p100,
            p001 to p101,
            p010 to p110,
            p011 to p111
        )

        val boundaries = listOf(
            minY,
            minY + stripeH,
            minY + stripeH * 2,
            minY + stripeH * 3,
            minY + stripeH * 4,
            maxY
        )

        for ((start, end) in edges) {

            val points = mutableListOf<Pair<Double, Vec3>>()
            points += 0.0 to start
            points += 1.0 to end

            val dy = end.y - start.y

            if (dy != 0.0) {
                for (y in boundaries.drop(1).dropLast(1)) {
                    val t = (y - start.y) / dy
                    if (t in 0.0..1.0) {
                        points += t to start.lerp(end, t)
                    }
                }
            }

            val sorted = points.sortedBy { it.first }

            for (i in 0 until sorted.size - 1) {
                val p0 = sorted[i].second
                val p1 = sorted[i + 1].second

                val midY = (p0.y + p1.y) / 2.0

                val stripe = (((midY - minY) / height) * 5.0)
                    .toInt()
                    .coerceIn(0, 4)

                drawLine(
                    listOf(p0, p1),
                    colors[stripe],
                    colors[stripe],
                    depth,
                    1f
                )
            }
        }
    }

    init {

        on<TickEvent.Server> {
            if (!M4State.inBoss()) return@on
            if (damaged > -1) {
                damaged--
            }
        }

        onReceive<ClientboundHurtAnimationPacket> {
            if (!M4State.inBoss()) return@onReceive
            val entity = OdinMod.mc.level!!.getEntity(this.id)
            if (entity is Ghast) {
                damaged = dmgFlashDuration
            }
        }

        on<RenderEvent.Extract> {
            if (!M4State.inBoss()) return@on
            runCatching {
                val style = renderStyle

                fun draw(list: Set<Entity>, color: Color) {
                    list.forEach { drawStyledBox(it.renderBoundingBox, color, style, true) }
                }

                if (highlightSheep) draw(sheep, sheepColor)
                if (highlightWolves) draw(wolves, wolfColor)
                if (highlightBats) draw(bats, batColor)
                if (highlightChicken) draw(chickens, chickenColor)
                if (highlightRabbit) draw(rabbits, rabbitColor)
                if (highlightCow) draw(cows, cowColor)

                if (highlightThorn) {
                    thorn?.let {
                        when {
                            thornDmgFlash && damaged >= 0 -> drawStyledBox(it.renderBoundingBox, thornDamagedColor, style, true)
                            transThorn -> drawTransBox(it.renderBoundingBox, style, true)
                            else -> drawStyledBox(it.renderBoundingBox, thornColor, style, true)
                        }
                    }
                }

                if (highlightBear && !(onlyShowBearOnMage && DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.MAGE)) {
                    bear?.let {
                        drawStyledBox(
                            if (noInterpolateBear) it.boundingBox else it.renderBoundingBox,
                            bearColor,
                            renderStyle,
                            true
                        )
                    }
                }
            }
        }
    }
}
