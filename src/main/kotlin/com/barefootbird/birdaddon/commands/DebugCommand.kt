package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.utils.Debug
import com.barefootbird.birdaddon.utils.modMessage
import com.github.stivais.commodore.Commodore

val debugCommand = Commodore("debugbird") {

    literal("debugmessages").runs {
        Debug.debugMessages = !Debug.debugMessages
        modMessage("Debug messages set to ${Debug.debugMessages}")
    }

    literal("disablebosschecks").runs {
        Debug.disableBossChecks = !Debug.disableBossChecks
        modMessage("Disable boss checks set to ${Debug.disableBossChecks}")
    }
}