package com.barefootbird.birdaddon.utils

import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.level.block.Blocks
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.modMessage
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.player.Player


object M4State {
    private inline val blockLocations get() = if (DungeonUtils.floor?.isMM == true) m4BlockLocations else f4BlockLocations
    inline val maxKills get() = if (DungeonUtils.floor?.isMM == true) 30 else 25
    private val lastBlockLocation = BlockPos(7, 77, 34)
    var bearTimer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    var kills = 0
    var timer = 0
    val bearSpawnRegex = Regex("^A Spirit Bear has appeared!$")
    val bearKillRegex = Regex("^The Spirit Bow has dropped!$")
    val bearSpawnTimes = mutableListOf<Int>()
    val bearKillTimes = mutableListOf<Int>()
    val bearSpawnStartTimes = mutableListOf<Int>()

    // The whole idea of this timer is to use events that are processed earlier in the tick than the block updates
    init {
        on<ChatPacketEvent> {
            if (bearSpawnRegex.matches(value)) {
                bearSpawnTimes.add(timer)
            }
            if (bearKillRegex.matches(value)) {
                bearKillTimes.add(timer)
                if (bearTimer != -1) {
                    bearTimer = -1
                    kills = 0
                }
            }
        }

        on<BlockUpdateEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss || !blockLocations.contains(pos)) return@on

            when (updated.block) {
                Blocks.SEA_LANTERN if old.block == Blocks.COAL_BLOCK -> {
                    if (kills < maxKills) {
                        val newKills = blockLocations.indexOf(pos) + 1
                        if (newKills > kills) {
                            kills = newKills
                        }
                    }
                    if (pos == lastBlockLocation && bearTimer == -1) {
                        bearTimer = 69
                        bearSpawnStartTimes.add(timer)
                    }
                }
            }
        }


        onReceive<ClientboundEntityEventPacket> {
            if (this.eventId.toInt() == 3) {
                val entity = this.getEntity(mc.level!!)

                // living entity death
                if (bearTimer == -1) {
                    var validDeath = false
                    when (entity) {
                        is Wolf -> validDeath = true
                        is Cow -> validDeath = true
                        is Rabbit -> validDeath = true
                        is Bat -> validDeath = true
                        is Sheep -> validDeath = true
                        is Chicken -> validDeath = true
                    }
                    if (validDeath) {
                        kills++ // temporarily updates the kills before the block update event is processed
                        if (kills >= maxKills) {
                            bearTimer = 70
                            bearSpawnStartTimes.add(timer)
                        }
                    }
                }
            }
        }

        on<TickEvent.Server> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss) return@on
            if (bearTimer > 0) bearTimer--
            timer++
        }

        on<WorldEvent.Load> {
            kills = 0
            bearTimer = -1
            timer = 0
            bearKillTimes.clear()
            bearSpawnStartTimes.clear()
            bearSpawnTimes.clear()
        }
    }

    private val f4BlockLocations = listOf(
        BlockPos(-3, 77, 33), BlockPos(-9, 77, 31), BlockPos(-16, 77, 26), BlockPos(-20, 77, 20), BlockPos(-23, 77, 13),
        BlockPos(-24, 77, 6), BlockPos(-24, 77, 0), BlockPos(-22, 77, -7), BlockPos(-18, 77, -13), BlockPos(-12, 77, -19),
        BlockPos(-5, 77, -22), BlockPos(1, 77, -24), BlockPos(8, 77, -24), BlockPos(14, 77, -23), BlockPos(21, 77, -19),
        BlockPos(27, 77, -14), BlockPos(31, 77, -8), BlockPos(33, 77, -1), BlockPos(34, 77, 5), BlockPos(33, 77, 12),
        BlockPos(31, 77, 19), BlockPos(27, 77, 25), BlockPos(20, 77, 30), BlockPos(14, 77, 33), BlockPos(7, 77, 34)
    )

    private val m4BlockLocations = listOf(
        BlockPos(-2, 77, 33), BlockPos(-7, 77, 32), BlockPos(-13, 77, 28), BlockPos(-17, 77, 24), BlockPos(-21, 77, 18),
        BlockPos(-23, 77, 13), BlockPos(-24, 77, 7), BlockPos(-24, 77, 2), BlockPos(-23, 77, -4), BlockPos(-21, 77, -9),
        BlockPos(-17, 77, -14), BlockPos(-12, 77, -19), BlockPos(-6, 77, -22), BlockPos(-1, 77, -23), BlockPos(5, 77, -24),
        BlockPos(10, 77, -24), BlockPos(16, 77, -22), BlockPos(21, 77, -19), BlockPos(27, 77, -15), BlockPos(30, 77, -10),
        BlockPos(32, 77, -5), BlockPos(34, 77, 1), BlockPos(34, 77, 7), BlockPos(33, 77, 12), BlockPos(31, 77, 18),
        BlockPos(28, 77, 23), BlockPos(23, 77, 28), BlockPos(18, 77, 31), BlockPos(12, 77, 33), BlockPos(7, 77, 34)
    )
}