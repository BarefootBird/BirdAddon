package com.barefootbird.birdaddon.utils.state

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf

object OverkillTracker {

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
            if (!M4State.inBoss() || eventId.toInt() != 3 || M4State.bearTimer == -1) {
                return@onReceive
            }

            val level = mc.level ?: return@onReceive
            val entity = getEntity(level) ?: return@onReceive

            overkill[entity.javaClass] = overkill.getValue(entity.javaClass) + 1
        }

        on<LevelEvent.Load> {
            reset()
        }
    }

    private fun reset() {
        overkill.keys.forEach { overkill[it] = 0 }
    }
}