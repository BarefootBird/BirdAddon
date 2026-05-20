package com.barefootbird.birdaddon.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.level.block.Blocks


object Islands {

    var onCgm4 = false
    var onM4Miku = false
    var onPrivateIs = false

    private val mikupattern = listOf(
        "##@##",
        "##@@#",
        "#@.#@",
        "@@###",
        "@@###"
    )

    private val cgm4pattern = listOf(
        "##@@#",
        "%#@@#",
        "#%.#@",
        "@@@@@",
        "@@##@"
    )

    val blockMap = mapOf(
        '#' to Blocks.COARSE_DIRT,
        '@' to Blocks.DIRT,
        '.' to Blocks.BROWN_STAINED_GLASS,
    )

    fun checkPattern(startPos: BlockPos, pattern: List<String>): Boolean {
        for (row in pattern.indices) {
            for (col in pattern[row].indices) {

                val symbol = pattern[row][col]

                val expectedBlock = blockMap[symbol] ?: continue

                val pos = BlockPos(
                    startPos.x - col,
                    startPos.y,
                    startPos.z - row
                )

                val actualBlock = mc.level?.getBlockState(pos)?.block

                if (actualBlock != expectedBlock) {
                    return false
                }
            }
        }
        return true
    }

    init {
        on<WorldEvent.Load> {
            onCgm4 = false
            onM4Miku = false
            onPrivateIs = false
        }

        on<TickEvent.Server> {
            if (!onPrivateIs) return@on
            if (onCgm4 || onM4Miku) return@on
            if (checkPattern(BlockPos(7, 68, 7), mikupattern)) onM4Miku = true
            if (checkPattern(BlockPos(9, 109, 9), cgm4pattern)) onCgm4 = true
            if (onM4Miku) {
                modMessage(onM4Miku)
            }
        }

        onReceive<ClientboundPlayerInfoUpdatePacket> { event ->
            val packet = event.packet
            if (packet is ClientboundPlayerInfoUpdatePacket) {
                for (entry in packet.entries()) {
                    if (entry.displayName?.string?.contains("Private Island") == true) {
                        onPrivateIs = true
                    }
                }
            }
        }
    }
}