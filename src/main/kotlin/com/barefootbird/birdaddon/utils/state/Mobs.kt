package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.features.impl.m4.Highlight
import com.odtheking.odin.OdinMod
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import kotlin.jvm.java

object Mobs {

    val animalTypes = listOf(
        Cow::class.java, Sheep::class.java, Chicken::class.java,
        Bat::class.java, Wolf::class.java, Rabbit::class.java
    )

    fun mobColorFor(entity: Class<out Entity>) = when (entity) {
        Cow::class.java -> Highlight.cowColor
        Sheep::class.java -> Highlight.sheepColor
        Chicken::class.java -> Highlight.chickenColor
        Bat::class.java -> Highlight.batColor
        Wolf::class.java -> Highlight.wolfColor
        Rabbit::class.java -> Highlight.rabbitColor
        else -> Colors.WHITE
    }

    fun mobNameFor(entity: Class<out Entity>, short: Boolean = false): String {
        return if (short) {
            when (entity) {
                Bat::class.java -> "B"
                Chicken::class.java -> "Ch"
                Cow::class.java -> "C"
                Sheep::class.java -> "S"
                Rabbit::class.java -> "R"
                Wolf::class.java -> "W"
                else -> "N/A"
            }
        } else {
            when (entity) {
                Bat::class.java -> "Bats"
                Chicken::class.java -> "Chickens"
                Cow::class.java -> "Cows"
                Sheep::class.java -> "Sheep"
                Rabbit::class.java -> "Rabbits"
                Wolf::class.java -> "Wolves"
                else -> "N/A"
            }
        }
    }

    private var animals: Map<Class<out Entity>, Set<Entity>> = emptyMap()

    fun animalsOf(type: Class<out Entity>): Set<Entity> = animals[type].orEmpty()

    private var _bows = emptySet<ArmorStand>()
    private var _thorn: Ghast? = null
    private var _bear: Player? = null
    val bows get() = _bows
    val thorn: Ghast? get() = _thorn
    val bear: Player? get() = _bear

    private val searchBox = AABB(-36.0, 68.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    init {

        on<LevelEvent.Load> {
            animals = emptyMap()
            _bows = emptySet()
            _thorn = null
            _bear = null
        }

        on<TickEvent.Server> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            runCatching {

                val animalsNew = animalTypes.associateWith { mutableSetOf<Entity>() }

                val bowsNew = mutableSetOf<ArmorStand>()
                var thornNew: Ghast? = null
                var bearNew: Player? = null

                val allEntities = OdinMod.mc.level
                    ?.getEntities(null, searchBox)
                    ?: return@runCatching

                allEntities.forEach { entity ->
                    if (!entity.isAlive) return@forEach

                    when (entity) {
                        is ArmorStand -> if (entity.name.string == "Spirit Bow") bowsNew.add(entity)
                        is Ghast -> thornNew = entity
                        is Player -> if (entity.gameProfile.name.equals("Spirit Bear")) bearNew = entity
                    }

                    val type = animalTypes.firstOrNull { it.isInstance(entity) } ?: return@forEach
                    if (entity is Bat && entity.isInvisible) return@forEach

                    animalsNew.getValue(type).add(entity)
                }

                animals = animalsNew.mapValues { (_, entities) -> entities.toSet() }
                _bows = bowsNew
                _thorn = thornNew
                _bear = bearNew
            }
        }
    }
}