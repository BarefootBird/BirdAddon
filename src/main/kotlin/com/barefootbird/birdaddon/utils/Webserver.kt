package com.barefootbird.birdaddon.utils

import com.odtheking.odin.OdinMod.mc
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.concurrent.thread


object Webserver {

    val webFolder = File(mc.gameDirectory, "m4logs").apply { mkdirs() }

    fun downloadFile(url: String, target: File) {
        URL(url).openStream().use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyChecksum(file: File, expected: String): Boolean {
        return sha256(file).equals(expected, ignoreCase = true)
    }

    fun unzip(zipFile: File, targetDir: File) {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(targetDir, entry.name)

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    fun sendClickableLink(url: String, text: String) {
        val uri = URI(url)

        val component = Component.literal(text)
            .withStyle(
                Style.EMPTY
                    .withClickEvent(ClickEvent.OpenUrl(uri))
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
            )



        mc.player?.displayClientMessage(component, false)
    }

    fun setupWebserver() {
        val zipUrl = "https://github.com/BarefootBird/M4MobSpawns/releases/download/a/webserver.zip"
        val zipFile = File(webFolder, "webserver.zip")
        val expectedHash = "e926d8cd5dde88529dbf706a08ad487df3d0760cd52b5c502fe3aabc2bf1285c"

        if (!serverFile.exists()) {
            modMessage("Downloading webserver...")

            downloadFile(zipUrl, zipFile)

            if (!verifyChecksum(zipFile, expectedHash)) {
                zipFile.delete()
                error("Checksum mismatch! Aborting.")
            }

            modMessage("Extracting webserver...")
            unzip(zipFile, webFolder)

            zipFile.delete()

            modMessage("Webserver installed.")
        }
    }

    fun getNodeBinary(): File {
        val os = System.getProperty("os.name").lowercase()

        val relativePath = when {
            os.contains("win") -> "node-binaries/win/node.exe"
            os.contains("mac") || os.contains("darwin") -> "node-binaries/mac/node"
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> "node-binaries/linux/node"
            else -> error("Unsupported OS: $os")
        }

        val file = File(webFolder, relativePath)

        if (!os.contains("win")) {
            file.setExecutable(true)
        }

        return file
    }

    val nodeExecutable = getNodeBinary()
    val serverFile = File(webFolder, "server.js")

    val pb = ProcessBuilder(
        nodeExecutable.absolutePath,
        serverFile.absolutePath
    )
        .directory(webFolder)
        .redirectOutput(File(webFolder, "webserver.log"))
        .redirectError(File(webFolder, "webserver-error.log"))

    var process: Process? = null


    fun startWebserver() {
        if (process != null && process!!.isAlive) {
            modMessage("Web Server already running")
            return
        }
        thread (start = true, isDaemon = true) {
            try {
                setupWebserver()
                process = pb.start()
                modMessage("Starting m4 webserver on http://localhost:4000")
                sendClickableLink("http://localhost:4000", "Open Website")
            } catch (e: Exception) {
                e.printStackTrace()
                modMessage("Failed to start webserver: ${e.message}")
            }
        }

    }

    fun stopWebserver() {
        modMessage("Stopping m4 webserver")
        process?.destroy()
        process = null
    }

    init {
        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientStopping {
            stopWebserver()
        })
    }
}