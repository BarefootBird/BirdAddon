package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes


object Particles: Module(
    name = "Particle hider",
    description = "hides annoying particles in m4 boss",
    category = Category.M4
) {
    private val hideAll by BooleanSetting("Hide All", true, desc = "Hides all the particles")
    private val healerCircle by BooleanSetting("Except Healer Circle", true, desc = "Shows healer circle particles even when hide particles is on") // HAPPY_VILLAGER
    private val mageBeam by BooleanSetting("Except Mage Beam", true, desc = "Shows mage beam particles even when hide particles is on") // FIREWORK



    @JvmStatic
    fun shouldHideParticle(particleOptions: ParticleOptions): Boolean {
        if (!enabled || !M4State.inBoss() || !hideAll) return false
        val type = particleOptions.type
        if (type == ParticleTypes.HAPPY_VILLAGER && healerCircle) return false
        if (type == ParticleTypes.FIREWORK && mageBeam) return false
        return true
    }
}