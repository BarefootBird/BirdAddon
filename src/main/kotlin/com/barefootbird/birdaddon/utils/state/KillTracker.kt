package com.barefootbird.birdaddon.utils.state

import com.barefootbird.birdaddon.events.EventDispatcher
import com.barefootbird.birdaddon.events.M4Event
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.level.block.Blocks

object KillTracker {

    private inline val blockLocations get() = if (DungeonUtils.floor?.isMM == true) m4BlockLocations else f4BlockLocations // coal/sea lantern blocks
    inline val maxKills get() = if (DungeonUtils.floor?.isMM == true) 30 else 25
    private val lastBlockLocation = BlockPos(7, 77, 34) // Last sea lantern/coal block
    var kills = 0

    val overkill: MutableMap<Class<out Entity>, Int> = linkedMapOf(
        Bat::class.java to 0,
        Chicken::class.java to 0,
        Cow::class.java to 0,
        Sheep::class.java to 0,
        Rabbit::class.java to 0,
        Wolf::class.java to 0
    )

    init {
        onReceive<ClientboundEntityEventPacket> {
            if (!BossLifecycle.inBoss() || eventId.toInt() != 3) { // event id 3 = mob death
                return@onReceive
            }

            val level = mc.level ?: return@onReceive
            val entity = getEntity(level) ?: return@onReceive
            if (Mobs.animalTypes.none { it.isInstance(entity)}) return@onReceive

            if (!BearTracker.bearSpawning() && !BearTracker.bearSpawned()) {
                kills++
                if (kills >= maxKills) {
                    EventDispatcher.triggerBearSpawnStart()
                }
            } else {
                overkill[entity.javaClass] = overkill.getValue(entity.javaClass) + 1
            }
        }

        // Update timer based on sea lantern/coal blocks
        on<BlockUpdateEvent> {
            if (!BossLifecycle.inBoss() || !blockLocations.contains(pos)) return@on

            when (updated.block) {
                Blocks.SEA_LANTERN if old.block == Blocks.COAL_BLOCK -> {
                    // Kill detected from sea lantern, update kills if they aren't already updated from entity kill packets
                    if (kills < maxKills) {
                        val newKills = blockLocations.indexOf(pos) + 1
                        if (newKills > kills) {
                            kills = newKills
                        }
                    }
                    // Final sea lantern is on, bear is starting to spawn
                    if (pos == lastBlockLocation) {
                        EventDispatcher.triggerBearSpawnStart()
                    }
                }
            }
        }

        on<M4Event.BearKill> {
            kills = 0
        }

        on<LevelEvent.Load> {
            reset()
        }
    }

    private fun reset() {
        overkill.keys.forEach { overkill[it] = 0 }
        kills = 0
    }

    // Locations for sea lantern/coal ring
    // Not every block on the ring is in here. Each kill updates like 4 blocks ish (different between m4 and f4) so its just 1 of those blocks per entry
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