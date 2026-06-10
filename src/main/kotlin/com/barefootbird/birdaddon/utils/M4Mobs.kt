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

    private var _sheep = emptySet<Sheep>()
    private var _wolves = emptySet<Wolf>()
    private var _bats = emptySet<Bat>()
    private var _chickens = emptySet<Chicken>()
    private var _rabbits = emptySet<Rabbit>()
    private var _cows = emptySet<Cow>()

    val sheep get() = _sheep
    val wolves get() = _wolves
    val bats get() = _bats
    val chickens get() = _chickens
    val rabbits get() = _rabbits
    val cows get() = _cows

    private var _thorn: Ghast? = null
    private var _bear: Player? = null
    val thorn: Ghast? get() = _thorn
    val bear: Player? get() = _bear
    private val searchBox = AABB(-36.0, 68.0, -36.0, 47.0, 110.0, 47.0) // m4 arena size

    init {

        on<WorldEvent.Load> {
            _sheep = emptySet()
            _wolves = emptySet()
            _bats = emptySet()
            _chickens = emptySet()
            _rabbits = emptySet()
            _cows = emptySet()
            _thorn = null
            _bear = null
        }

        on<TickEvent.Server> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on

            runCatching {

                val sheepNew = mutableSetOf<Sheep>()
                val wolvesNew = mutableSetOf<Wolf>()
                val batsNew = mutableSetOf<Bat>()
                val chickensNew = mutableSetOf<Chicken>()
                val rabbitsNew = mutableSetOf<Rabbit>()
                val cowsNew = mutableSetOf<Cow>()
                var thornNew: Ghast? = null
                var bearNew: Player? = null

                val allEntities = OdinMod.mc.level
                    ?.getEntities(null, searchBox)
                    ?: return@runCatching

                allEntities.forEach { entity ->
                    if (!entity.isAlive) return@forEach

                    when (entity) {
                        is Sheep -> sheepNew.add(entity)
                        is Wolf -> wolvesNew.add(entity)
                        is Chicken -> chickensNew.add(entity)
                        is Rabbit -> rabbitsNew.add(entity)
                        is Cow -> cowsNew.add(entity)

                        is Ghast -> thornNew = entity

                        is Bat -> if (!entity.isInvisible) batsNew.add(entity)

                        is Player -> if (entity.gameProfile.name.equals("Spirit Bear")) bearNew = entity
                    }
                }

                _sheep = sheepNew
                _wolves = wolvesNew
                _bats = batsNew
                _chickens = chickensNew
                _rabbits = rabbitsNew
                _cows = cowsNew
                _thorn = thornNew
                _bear = bearNew
            }
        }
    }
}