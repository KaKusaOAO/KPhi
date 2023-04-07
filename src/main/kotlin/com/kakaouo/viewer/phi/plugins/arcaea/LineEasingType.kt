package com.kakaouo.viewer.phi.plugins.arcaea

import com.kakaouo.viewer.phi.Vector2

enum class LineEasingType(val id: String, private val xEase: ArcaeaEasingType, private val yEase: ArcaeaEasingType = xEase) {
    S("s", ArcaeaEasingType.LINEAR),
    B("b", ArcaeaEasingType.BEZIER),

    SI("si", ArcaeaEasingType.CIRCLE_IN, ArcaeaEasingType.LINEAR),
    SO("so", ArcaeaEasingType.CIRCLE_OUT, ArcaeaEasingType.LINEAR),

    SISI("sisi", ArcaeaEasingType.CIRCLE_IN, ArcaeaEasingType.CIRCLE_IN),
    SISO("siso", ArcaeaEasingType.CIRCLE_IN, ArcaeaEasingType.CIRCLE_OUT),

    SOSI("sosi", ArcaeaEasingType.CIRCLE_OUT, ArcaeaEasingType.CIRCLE_IN),
    SOSO("soso", ArcaeaEasingType.CIRCLE_OUT, ArcaeaEasingType.CIRCLE_OUT);

    companion object {
        private val idMap = mutableMapOf<String, LineEasingType>()

        fun of(name: String): LineEasingType {
            return idMap[name] ?: S
        }
    }

    init {
        this.also {
            idMap[id] = it
        }
    }

    fun interpolate(start: Vector2, end: Vector2, progress: Double): Vector2 {
        return Vector2(
            xEase.interpolate(start.x, end.x, progress),
            yEase.interpolate(start.y, end.y, progress)
        )
    }

    fun interpolateX(start: Double, end: Double, progress: Double): Double {
        return xEase.interpolate(start, end, progress)
    }

    fun interpolateY(start: Double, end: Double, progress: Double): Double {
        return yEase.interpolate(start, end, progress)
    }
}

