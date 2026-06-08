package com.barefootbird.birdaddon.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

fun modMessage(message: Component, prefix: String = "§bBird Addon §8»§r ", chatStyle: Style? = null) {
    val text = Component.literal(prefix).append(message)
    chatStyle?.let { text.setStyle(chatStyle) }
    mc.execute { mc.gui.chat.addMessage(text) }
}

fun modMessage(message: Any?, prefix: String = "§bBird Addon §8»§r ", chatStyle: Style? = null) {
    val text = Component.literal("$prefix$message")
    chatStyle?.let { text.setStyle(chatStyle) }
    mc.execute { mc.gui.chat.addMessage(text) }
}

fun debugMessage(message: Component, prefix: String = "§bDebug §8»§r ", chatStyle: Style? = null) {
    if (Debug.debugMessages) {
        modMessage(message, prefix, chatStyle)
    }
}

fun debugMessage(message: Any?, prefix: String = "§bBird Addon §8»§r ", chatStyle: Style? = null) {
    if (Debug.debugMessages) {
        modMessage(message, prefix, chatStyle)
    }
}

fun sendCommand(command: String) {
    mc.execute { mc.player?.connection?.sendCommand(command) }
}