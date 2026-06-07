package com.barefootbird.birdaddon.events

import com.odtheking.odin.events.core.Event

// Class to represent M4 Events
abstract class M4Event() : Event {
    class BearKill : M4Event()
    class BearSpawn : M4Event()
    class BearSpawnStart : M4Event()
    class End: M4Event()
}