package com.barefootbird.birdaddon.features.impl.m4

import com.barefootbird.birdaddon.utils.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import java.util.UUID

object FrankBlinder: Module(
    name = "Frank Blinder",
    description = "blinds frank",
    category = Category.M4
) {
    private val hud by HUD(name, "", false) {
        val player = mc.player ?: return@HUD 0 to 0
        if (player.uuid != FRANK_UUID)
            return@HUD 0 to 0

        this.fill(
            -50,
            -50,
            mc.window.guiScaledWidth+50,
            mc.window.guiScaledHeight+50,
            Colors.BLACK.rgba
        )

        0 to 0
    }

    private val FRANK_UUID = UUID.fromString("f2378ee5-5d50-49fd-99f8-3a90f15f61ce")
}