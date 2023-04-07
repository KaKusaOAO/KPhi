package com.kakaouo.viewer.phi.plugins.arcaea

import com.kakaouo.viewer.phi.clamp
import kotlin.math.pow

enum class ArcaeaEasingType(private val equation: (Double, Double, Double) -> Double) {
    /**
     * Linear interpolation. Encoded as `s`.
     */
    LINEAR({ a, b, t -> com.kakaouo.viewer.phi.lerp(a, b, t) }),

    /**
     * Circle out easing. Encoded as `so`.
     */
    CIRCLE_OUT({ a, b, t ->
        a + (b - a) * (1 - kotlin.math.cos(kotlin.math.PI / 2 * t));
    }),

    /**
     * Circle in easing. Encoded as `si`.
     */
    CIRCLE_IN({ a, b, t ->
        a + (b - a) * (kotlin.math.sin(kotlin.math.PI / 2 * t));
    }),

    /**
     * Bezier out easing. Encoded as `b`.
     */
    BEZIER({ a, b, t ->
        val o = 1 - t;
        // a^3 * n0 + 3a^2b * n0 + 3ab^2 * n1 + b^3 * n1
        o.pow(3) * a +
                3 * o.pow(2) * t * a +
                3 * o * t.pow(2) * b +
                t.pow(3) * b
    });

    fun interpolate(start: Double, end: Double, progress: Double): Double {
        return equation(start, end, progress.clamp(0.0, 1.0))
    }
}