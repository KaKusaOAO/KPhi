package com.kakaouo.viewer.phi

import kotlin.math.max
import kotlin.math.min

fun lerp(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}

/**
 * Clamp `t` between `a` and `b`.
 */
fun clamp(t: Double, a: Double, b: Double): Double {
    return max(a, min(b, t))
}

fun Double.lerp(a: Double, b: Double): Double = lerp(a, b, this)
fun Double.clamp(min: Double, max: Double): Double = clamp(this, min, max)