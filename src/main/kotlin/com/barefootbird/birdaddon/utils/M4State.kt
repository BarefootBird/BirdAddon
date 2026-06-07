package com.barefootbird.birdaddon.utils

import com.barefootbird.birdaddon.events.EventDispatcher
import com.barefootbird.birdaddon.events.M4Event
import com.barefootbird.birdaddon.features.impl.m4.Logging.writeKills
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.level.block.Blocks
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@OptIn(DelicateCoroutinesApi::class)
object M4State {

    private inline val blockLocations get() = if (DungeonUtils.floor?.isMM == true) m4BlockLocations else f4BlockLocations // coal/sea lantern blocks
    inline val maxKills get() = if (DungeonUtils.floor?.isMM == true) 30 else 25
    private val lastBlockLocation = BlockPos(7, 77, 34) // Last sea lantern/coal block

    var bearTimer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    var kills = 0
    var timer = 0 // Times ticks from the start of boss

    val enteredRegex = Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$")
    val endRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")

    // These track times in ticks for the events
    val bearSpawnTimes = mutableListOf<Int>()
    val bearKillTimes = mutableListOf<Int>()
    val bearSpawnStartTimes = mutableListOf<Int>()

    // Usually I just use odin's DungeonUtils for this, but odin's tick timer uses a different system, so I use this
    // to make sure that the odin timer and my timer match exactly
    var inThornBoss = false

    var overkill = 0
    var overkillBats = 0
    var overkillChickens = 0
    var overkillCows = 0
    var overkillSheep = 0
    var overkillRabbits = 0
    var overkillWolves = 0

    var ended = false

    // Default spot in the middle, bear can't ever actually spawn here, but this is the average of all bear spawns if you let thorn move freely
    var bearSpawnSpot: Vec2 = Vec2(5.5,5.5)

    fun inBoss (): Boolean {
        if (Debug.disableBossChecks) return false
        return DungeonUtils.inBoss && DungeonUtils.isFloor(4)
    }

    fun updateBearSpawnSpot () {
        if (bearTimer == -1) {
            if (M4Mobs.ghasts.isEmpty()) return
            val thorn = M4Mobs.ghasts.find { true }!!

            // Bear spawns roughly on a circle around (5.5, 5.5) with a radius of 0.7,
            // and spawns on the closest point to where thorn was when the bear started spawning
            val dx = thorn.x - 5.5
            val dz = thorn.z - 5.5

            val angle = atan2(dz, dx)

            val r = 0.7
            val newX = 5.5 + r * cos(angle)
            val newZ = 5.5 + r * sin(angle)

            bearSpawnSpot = Vec2(newX, newZ)
        }
    }

    // updates and logs the kills
    fun updateKills (kills: Int) {
        this.kills = kills
        if (inThornBoss) {
            writeKills(kills)
        }
    }

    init {
        on<M4Event.BearSpawn> {
            bearSpawnTimes.add(timer)
        }

        on<M4Event.BearKill> {
            bearKillTimes.add(timer)
            bearTimer = -1
            updateKills(0)
        }

        on<M4Event.BearSpawnStart> {
            bearTimer = 70
            bearSpawnStartTimes.add(timer)
        }

        on<ChatPacketEvent> {
            // Boss checks
            if (enteredRegex.matches(value)) {
                inThornBoss = true
            }
            if (!inThornBoss) return@on
        }

        // Update timer based on sea lantern/coal blocks
        on<BlockUpdateEvent> {
            if (!inThornBoss || !blockLocations.contains(pos)) return@on

            when (updated.block) {
                Blocks.SEA_LANTERN if old.block == Blocks.COAL_BLOCK -> {
                    // Kill detected from sea lantern, update kills if they aren't already updated from entity kill packets
                    if (kills < maxKills) {
                        val newKills = blockLocations.indexOf(pos) + 1
                        if (newKills > kills) {
                            updateKills(newKills)
                        }
                    }
                    // Final sea lantern is on, bear is starting to spawn
                    if (pos == lastBlockLocation) {
                        EventDispatcher.triggerBearSpawnStart()
                    }
                }
            }
        }

        // The Entity Event Packet is processed ~10ms before the block update packet
        onReceive<ClientboundEntityEventPacket> {
            if (!inThornBoss) return@onReceive
            if (this.eventId.toInt() == 3) { // Event ID 3 is death
                val entity = this.getEntity(mc.level!!)

                // living entity death
                if (entity is Wolf || entity is Cow || entity is Rabbit ||
                    entity is Bat || entity is Sheep || entity is Chicken
                ) {
                    if (bearTimer == -1) {

                        updateKills(kills + 1)
                        if (kills >= maxKills) {
                            EventDispatcher.triggerBearSpawnStart()
                        }
                    } else {
                        overkill++
                        if (entity is Wolf) overkillWolves++
                        if (entity is Cow) overkillCows++
                        if (entity is Rabbit) overkillRabbits++
                        if (entity is Sheep) overkillSheep++
                        if (entity is Chicken) overkillChickens++
                        if (entity is Bat) overkillBats++
                    }
                }
            }
        }

        on<TickEvent.Server> {
            if (!inThornBoss) return@on
            if (ended) return@on
            if (bearTimer > 0) bearTimer--
            timer++
            updateBearSpawnSpot()
        }

        on<M4Event.End> {
            ended = true
        }

        on<WorldEvent.Load> {
            // Reset state on world load
            updateKills(0)
            bearTimer = -1
            timer = 0
            bearKillTimes.clear()
            bearSpawnStartTimes.clear()
            bearSpawnTimes.clear()
            overkill = 0
            overkillBats = 0
            overkillChickens = 0
            overkillCows = 0
            overkillSheep = 0
            overkillRabbits = 0
            overkillWolves = 0
            ended = false
            inThornBoss = false
        }
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