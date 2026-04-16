package com.barefootbird.birdaddon.commands

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.utils.modMessage
import java.io.File
import com.odtheking.odin.OdinMod.mc


val webFolder = File(mc.gameDirectory, "m4logs").apply { mkdirs() }
val exeFile = File(webFolder, "./m4mobspawns-webserver-win.exe")
val pb = ProcessBuilder(exeFile.absolutePath)
    .directory(webFolder)
    .redirectOutput(File(webFolder, "webserver.log"))
    .redirectError(File(webFolder, "webserver-error.log"))
var process: Process? = null

// Commands are handled via https://github.com/Stivais/Commodore
val startWebserverCommand = Commodore("startm4webserver") {
    runs {
        modMessage("Starting m4 webserver on http://localhost:3000")
        if (process == null) {
            process = pb.start()
        }
    }
}

// Commands are handled via https://github.com/Stivais/Commodore
val stopWebserverCommand = Commodore("stopm4webserver") {
    runs {
        modMessage("Stopping m4 webserver")
        process?.destroy()
    }
}