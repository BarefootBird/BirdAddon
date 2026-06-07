package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.barefootbird.birdaddon.utils.M4State
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module


object Sounds: Module(
    name = "Sounds",
    description = "Mutes annoying sounds in m4 boss",
    category = Category.M4
) {
    private val muteAnimalSounds by BooleanSetting("Mute animals", true, desc = "Mutes the sounds of m4 animals")
    private val muteThorn by BooleanSetting("Mute Thorn", true, desc = "Mutes thorn")
    private val muteMiscSounds by BooleanSetting("Mute Misc Sounds", true, desc = "Mutes stepping noises, bat/chicken spawning noises, thunder, explosion, zombie/skeleton hurt")


    private val animalSounds = setOf(
        "minecraft:entity.bat.death",
        "minecraft:entity.bat.hurt",
        "minecraft:entity.bat.loop",
        "minecraft:entity.bat.takeoff",
        "minecraft:entity.bat.ambient",

        "minecraft:entity.chicken.death",
        "minecraft:entity.chicken.hurt",
        "minecraft:entity.chicken.step",
        "minecraft:entity.chicken.egg",
        "minecraft:entity.chicken.ambient",

        "minecraft:entity.cow.death",
        "minecraft:entity.cow.hurt",
        "minecraft:entity.cow.ambient",
        "minecraft:entity.cow.step",

        "minecraft:entity.rabbit.attack",
        "minecraft:entity.rabbit.death",
        "minecraft:entity.rabbit.hurt",
        "minecraft:entity.rabbit.jump",
        "minecraft:entity.rabbit.ambient",

        "minecraft:entity.sheep.death",
        "minecraft:entity.sheep.hurt",
        "minecraft:entity.sheep.step",
        "minecraft:entity.sheep.ambient",

        "minecraft:entity.wolf.death",
        "minecraft:entity.wolf.growl",
        "minecraft:entity.wolf.howl",
        "minecraft:entity.wolf.hurt",
        "minecraft:entity.wolf.pant",
        "minecraft:entity.wolf.shake",
        "minecraft:entity.wolf.whine",
        "minecraft:entity.wolf.ambient",
        "minecraft:entity.wolf.step",
    )

    private val ghastSounds = setOf(
        "minecraft:entity.ghast.death",
        "minecraft:entity.ghast.hurt",
        "minecraft:entity.ghast.scream",
        "minecraft:entity.ghast.warn",
        "minecraft:entity.ghast.ambient",
    )

    private val miscSounds = setOf(
        "minecraft:block.fire.ambient",
        "minecraft:block.grass.step",
        "minecraft:block.glass.step",
        "minecraft:block.gravel.step",
        "minecraft:block.stone.step",
        "minecraft:block.wood.step",
        "minecraft:block.wool.step",
        "minecraft:entity.skeleton.hurt",
        "minecraft:entity.zombie.hurt",
        "minecraft:entity.zombie.infect",
        "minecraft:entity.lightning_bolt.thunder",
        "minecraft:entity.generic.explode",
    )

    @JvmStatic
    fun shouldBlockSound(soundId: String): Boolean {
        if (!enabled || !M4State.inBoss()) return false
        if (muteAnimalSounds && animalSounds.contains(soundId)) return true
        if (muteThorn && ghastSounds.contains(soundId)) return true
        if (muteMiscSounds && miscSounds.contains(soundId)) return true
        return false
    }
}