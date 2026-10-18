package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.state.M4Mobs
import com.barefootbird.birdaddon.utils.state.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf

object MobCounters : Module(
    name = "Mob Counter",
    key = null,
    description = "Shows how many mobs are alive in m4",
    category = Category.M4
) {

    private data class Counter(
        val enabled: Boolean,
        val color: Color,
        val prefix: String,
        val compactPrefix: String,
        val compactSeparate: Boolean,
        val amount: Int
    )


    private val totalGUI by BooleanSetting("Total Mobs", true, desc = "Show Total Mobs")
    private val cowsGUI by BooleanSetting("Cows", true, desc = "Shows alive cows")
    private val sheepGUI by BooleanSetting("Sheep", true, desc = "Shows alive sheep")
    private val chickensGUI by BooleanSetting("Chickens", true, desc = "Shows alive chickens")
    private val batsGUI by BooleanSetting("Bats", true, desc = "Shows alive bats")
    private val wolvesGUI by BooleanSetting("Wolves", true, desc = "Shows alive wolves")
    private val rabbitsGUI by BooleanSetting("Rabbits", true, desc = "Shows alive rabbits")
    private val mobsUnderThornGui by BooleanSetting(
        "Mobs Under Thorn",
        true,
        desc = "Shows mobs under thorn"
    )
    private val rabbitsNotUnderThornGui by BooleanSetting(
        "Rabbits Not Under Thorn",
        true,
        desc = "Shows the number of rabbits that aren't under thorn"
    )

    private val prefixes by BooleanSetting(
        "Prefixes",
        true,
        desc = "Adds a prefix for each of the mob types"
    )

    private val compactMode by BooleanSetting(
        "Compact Mode",
        false,
        desc = "Renders mob counters on one line with short mob names"
    )

    data class AnimalCounterConfig(
        val type: Class<out Entity>,
        val on: Boolean,
    )

    private val animalCounterConfigs = listOf(
        AnimalCounterConfig(Cow::class.java, cowsGUI),
        AnimalCounterConfig(Sheep::class.java, sheepGUI),
        AnimalCounterConfig(Chicken::class.java, chickensGUI),
        AnimalCounterConfig(Bat::class.java, batsGUI),
        AnimalCounterConfig(Wolf::class.java, wolvesGUI),
        AnimalCounterConfig(Rabbit::class.java, rabbitsGUI),
    )

    private val hud by HUD(name, "Mob Counter Hud", false) { example ->

        fun renderCounters(counters: List<Counter>): Pair<Int, Int> {
            val visibleCounters = counters.filter { it.enabled }

            if (compactMode) {
                val compactCounters = visibleCounters.filter { !it.compactSeparate }
                val separateCounters = visibleCounters.filter { it.compactSeparate }
                var width = 0
                var height = if (compactCounters.isEmpty()) 0 else 9
                var x = 0

                compactCounters.forEach { counter ->
                    val text = "${counter.compactPrefix}: ${counter.amount}"
                    val (textWidth, _) = textDim(text, x, 0, counter.color)
                    x += textWidth + 4
                }

                width = maxOf(width, x)

                separateCounters.forEach { counter ->
                    val text = if (prefixes) {
                        counter.prefix + counter.amount
                    } else {
                        counter.amount.toString()
                    }
                    val (lineWidth, lineHeight) = textDim(text, 0, height, counter.color)
                    width = maxOf(width, lineWidth)
                    height += lineHeight
                }

                return width to height
            }

            var width = 0
            visibleCounters.forEachIndexed { index, counter ->
                val text = if (prefixes) {
                    counter.prefix + counter.amount
                } else {
                    counter.amount.toString()
                }
                val (lineWidth, _) = textDim(text, 0, index * 9, counter.color)
                width = maxOf(width, lineWidth)
            }

            return width to visibleCounters.size * 9
        }

        if (example) {
            val examples = listOf(
                Counter(totalGUI, Colors.WHITE, "Mobs: ", "M", false, 0),
                Counter(cowsGUI, Highlight.cowColor, "Cows: ", "C", false, 0),
                Counter(sheepGUI, Highlight.sheepColor, "Sheep: ", "S", false, 0),
                Counter(chickensGUI, Highlight.chickenColor, "Chickens: ", "Ch", false, 0),
                Counter(batsGUI, Highlight.batColor, "Bats: ", "B", false, 0),
                Counter(wolvesGUI, Highlight.wolfColor, "Wolves: ", "W", false, 0),
                Counter(rabbitsGUI, Highlight.rabbitColor, "Rabbits: ", "R", false, 0),
                Counter(mobsUnderThornGui, Highlight.thornColor, "Under Thorn: ", "UT", true, 0),
                Counter(rabbitsNotUnderThornGui, Highlight.rabbitColor, "Rabbits in Mid: ", "RM", true, 0)
            )

            return@HUD renderCounters(examples)
        }

        if (!M4State.inBoss()) {
            return@HUD 0 to 0
        }

        val animalCounters = animalCounterConfigs.map {
            Counter(
                it.on,
                M4Mobs.mobColorFor(it.type),
                M4Mobs.mobNameFor(it.type),
                M4Mobs.mobNameFor(it.type, short = true),
                false,
                M4Mobs.animalsOf(it.type).size
            )
        }

        val allMobs = M4Mobs.animalTypes.flatMap { M4Mobs.animalsOf(it) }

        val mobsUnderThorn = allMobs.count {
            isUnderThorn(it.x, it.y, it.z)
        }

        val rabbitsInMid = M4Mobs.animalsOf(Rabbit::class.java).count {
            !isUnderThorn(it.x, it.y, it.z)
        }

        val counters = buildList {
            add(Counter(totalGUI, Colors.WHITE, "Mobs: ", "M", false, allMobs.size))
            addAll(animalCounters)
            add(Counter(mobsUnderThornGui, Highlight.thornColor, "Under Thorn: ", "UT", true, mobsUnderThorn))
            add(Counter(rabbitsNotUnderThornGui, Highlight.rabbitColor, "Rabbits in Mid: ", "RM", true, rabbitsInMid))
        }

        return@HUD renderCounters(counters)
    }

    private fun isUnderThorn(x: Double, y: Double, z: Double): Boolean {
        return y < 75 && ((x > 21 && z > 21) || (x > 26 && z > 16) || (x > 16 && z > 26))
    }
}
