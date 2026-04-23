package com.barefootbird.birdaddon.utils

import com.odtheking.odin.OdinMod
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

object M4Mobs {
    val totalSheep = mutableSetOf<Sheep>()
    val totalWolves = mutableSetOf<Wolf>()
    val totalBats = mutableSetOf<Bat>()
    val totalChickens = mutableSetOf<Chicken>()
    val totalRabbits = mutableSetOf<Rabbit>()
    val totalCows = mutableSetOf<Cow>()
    val sheep = mutableSetOf<Sheep>()
    val wolves = mutableSetOf<Wolf>()
    val bats = mutableSetOf<Bat>()
    val chickens = mutableSetOf<Chicken>()
    val rabbits = mutableSetOf<Rabbit>()
    val cows = mutableSetOf<Cow>()
    val ghasts = mutableSetOf<Ghast>()
    val bears = mutableSetOf<Player>()
    private val searchBox = AABB(-36.0, -36.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size


    init {

        on<WorldEvent.Load> {
            sheep.clear()
            wolves.clear()
            bats.clear()
            chickens.clear()
            rabbits.clear()
            cows.clear()
            ghasts.clear()
        }

        on<TickEvent.Server> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            runCatching {
                val allEntities = OdinMod.mc.level?.getEntities(null, searchBox)?.toList() ?: emptyList()

                sheep.clear()
                wolves.clear()
                bats.clear()
                chickens.clear()
                rabbits.clear()
                cows.clear()
                ghasts.clear()

                allEntities.forEach { entity ->
                    if (!entity.isAlive) return@forEach
                    val id = entity.id
                    when (entity) {
                        is Sheep -> {
                            if (M4State.timer >= 55 * 20) // try stop explo sheep messing with data
                                sheep.add(entity)
                            if (totalSheep.find { it.id == id } == null) {
                                totalSheep.add(entity)
                            }
                        }

                        is Wolf -> {
                            wolves.add(entity)
                            if (totalWolves.find { it.id == id } == null) {
                                totalWolves.add(entity)
                            }
                        }

                        is Bat -> {
                            if (M4State.timer >= 15 * 20) // try stop spirit scepter messing with data
                                if (entity.isInvisible) {
                                    totalBats.remove(entity)
                                } else {
                                    bats.add(entity)
                                    if (totalBats.find { it.id == id } == null) {
                                        totalBats.add(entity)
                                    }
                                }
                        }

                        is Chicken -> {
                            chickens.add(entity)
                            if (totalChickens.find { it.id == id } == null) {
                                totalChickens.add(entity)
                            }
                        }

                        is Rabbit -> {
                            rabbits.add(entity)
                            if (totalRabbits.find { it.id == id } == null) {
                                totalRabbits.add(entity)
                            }
                        }

                        is Cow -> {
                            cows.add(entity)
                            if (totalCows.find { it.id == id } == null) {
                                totalCows.add(entity)
                            }
                        }

                        is Ghast -> ghasts.add(entity)
                    }
                    if (entity is Player) {
                        if (entity.gameProfile.name.lowercase().startsWith("Spirit Bear")) {
                            bears.add(entity)
                        }
                    }
                }
            }
        }
    }
}