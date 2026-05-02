package com.barefootbird.birdaddon.utils

import com.barefootbird.birdaddon.features.impl.m4.Logging.writeKills
import com.barefootbird.birdaddon.features.impl.m4.Timer
import com.barefootbird.birdaddon.features.impl.m4.Titles
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@OptIn(DelicateCoroutinesApi::class)
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
    var onCgm4 = false
    var inThornBoss = false

    val enteredRegex = Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$")

    class DamagePacket constructor(
        var time: Long = 0,
        var pos: Vec3? = null
    )

    var damagePackets = mutableListOf<DamagePacket>()
    var lastServerTick: Long = 0
    var overkill = 0
    var overkillBats = 0
    var overkillChickens = 0
    var overkillCows = 0
    var overkillSheep = 0
    var overkillRabbits = 0
    var overkillWolves = 0
    val endRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    var ended = false
    var bearSpawnSpot = Vec3(5.5, 6.0, 5.5)

    fun updateBearSpawnSpot () {
        if (bearTimer == -1) {
            if (M4Mobs.ghasts.isEmpty()) return
            val thorn = M4Mobs.ghasts.find { true }!!

            val dx = thorn.x - 5.5
            val dz = thorn.z - 5.5

            val angle = atan2(dz, dx)

            val r = 0.7
            val newX = 5.5 + r * cos(angle)
            val newZ = 5.5 + r * sin(angle)

            bearSpawnSpot = Vec3(newX, 6.0, newZ)
        }
    }

    fun updateKills (kills: Int) {
        this.kills = kills
        if (inThornBoss) {
            writeKills(kills)
        }
    }


    // The whole idea of this timer is to use events that are processed earlier in the tick than the block updates
    init {
        on<ChatPacketEvent> {
            if (enteredRegex.matches(value)) {
                inThornBoss = true
            }
            if (!inThornBoss) return@on
            if (bearSpawnRegex.matches(value)) {
                bearSpawnTimes.add(timer)
                Titles.handleBearSpawn()
            }
            if (bearKillRegex.matches(value)) {
                bearKillTimes.add(timer)
                if (bearTimer != -1) {
                    bearTimer = -1
                    updateKills(0)
                    Titles.handleBearKill()
                }
            }
            if (endRegex.matches(value) && !ended) {
                ended = true
                if (Timer.printToChat) {
                    GlobalScope.launch {
                        delay(1000)
                        modMessage("Thorn Defeated in ${(timer / 20.0).toFixed(2)}s")
                    }
                }
                if (Timer.printLastBowTime) {
                    GlobalScope.launch {
                        delay(1000)
                        modMessage("Last Bow Shot In ${((timer - bearKillTimes[bearKillTimes.size - 1]) / 20.0).toFixed(2)}s")
                    }
                }
            }
        }

        on<BlockUpdateEvent> {
            if (!inThornBoss || !blockLocations.contains(pos)) return@on

            when (updated.block) {
                Blocks.SEA_LANTERN if old.block == Blocks.COAL_BLOCK -> {
                    if (kills < maxKills) {
                        val newKills = blockLocations.indexOf(pos) + 1
                        if (newKills > kills) {
                            updateKills(newKills)
                        }
                    }
                    if (pos == lastBlockLocation && bearTimer == -1) {
                        bearTimer = 69
                        bearSpawnStartTimes.add(timer)
                        Titles.handleBearSpawnStart()
                    }
                }
            }
        }

        onReceive<ClientboundAddEntityPacket> {
            val now = System.nanoTime()
            if (type == EntityType.ARMOR_STAND) {
                damagePackets.add(DamagePacket(now, Vec3(x, y, z)))
                // Just assume that every armor stand is a dmg splash
                // The ones that aren't dmg splashes probably aren't relevant anyway
            }
            runCatching {
                damagePackets.removeIf { now - it.time > 125_000_000 } // 125 ms
            }
        }

        onReceive<ClientboundEntityEventPacket> {
            if (!inThornBoss) return@onReceive
            if (this.eventId.toInt() == 3) {
                val entity = this.getEntity(mc.level!!)

                // living entity death
                if (entity is Wolf || entity is Cow || entity is Rabbit ||
                    entity is Bat || entity is Sheep || entity is Chicken
                ) {
                    if (bearTimer == -1) {

                        updateKills(kills + 1) // temporarily updates the kills before the block update event is processed
                        if (kills >= maxKills) {
                            bearTimer = 70
                            bearSpawnStartTimes.add(timer)
                            Titles.handleBearSpawnStart()
                            val damagePacket = damagePackets.find {
                                it.pos?.distanceTo(entity.position() )!! < 2.0
                            }
                            if (damagePacket != null) {

                                if (damagePacket.time < lastServerTick) {
                                    // The damage splash appeared before the last tick
                                    // Therefore the mob was counted as killed before the last tick
                                    // So 1 tick has already passed since the kill
                                    // This doesn't seem to work that well, or at least it needs further testing
                                    bearTimer--
                                }
                            }
                        }
                    }
                    else {
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
            val now = System.nanoTime()
            lastServerTick = now
            updateBearSpawnSpot()
        }

        on<WorldEvent.Load> {
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
            onCgm4 = false
            inThornBoss = false
        }

        onReceive<ClientboundSetPlayerTeamPacket> { event ->
            val packet = event.packet
            if (packet is ClientboundSetPlayerTeamPacket) {
                val opt = packet.parameters
                if (!opt.isPresent) return@onReceive
                val team = opt.get()
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return@onReceive
                if (!packet.name.matches(teamRegex)) return@onReceive
                val message = "${teamPrefix}${teamSuffix.trim()}".noControlCodes
                if (message.contains("catgirlm4")) {
                    onCgm4 = true
                }
            }
        }
    }
    private val teamRegex = "^team_(\\d+)$".toRegex()


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