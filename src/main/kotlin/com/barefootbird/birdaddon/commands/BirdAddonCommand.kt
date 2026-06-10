package com.barefootbird.birdaddon.commands

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.clickgui.ClickGUI
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.OdinMod.mc

val mainCommand = Commodore("birdaddon") {
    runs {
        schedule(0) { mc.setScreen(ClickGUI) }
    }
}