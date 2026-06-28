package com.barefootbird.birdaddon.utils

data class Vec2(val x: Double, val z: Double) {
    constructor(x: Int, z: Int) : this(x.toDouble(), z.toDouble())
}
