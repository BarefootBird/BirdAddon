package com.barefootbird.birdaddon.commands

import com.barefootbird.birdaddon.utils.Webserver.startWebserver
import com.barefootbird.birdaddon.utils.Webserver.stopWebserver
import com.github.stivais.commodore.Commodore

val startWebserverCommand = Commodore("startm4webserver") {
    runs {
        startWebserver()
    }
}

val stopWebserverCommand = Commodore("stopm4webserver") {
    runs {
        stopWebserver()
    }
}