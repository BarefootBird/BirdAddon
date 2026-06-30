package com.barefootbird.birdaddon.utils

import com.barefootbird.birdaddon.features.impl.m4.FrankBlinder
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on


object FrankMustAlwaysBeBlind {
    fun init() {
        on<RenderEvent.Extract> {
            if (!FrankBlinder.enabled){
                FrankBlinder.toggle() //teehee
            }
        }
    }
}