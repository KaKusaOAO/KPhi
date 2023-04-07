package com.kakaouo.viewer.phi

class Vector2(var x: Double, var y: Double) {
    operator fun plusAssign(other: Vector2) {
        x += other.x
        y += other.y
    }

    operator fun timesAssign(other: Double) {
        x *= other
        y *= other
    }
}