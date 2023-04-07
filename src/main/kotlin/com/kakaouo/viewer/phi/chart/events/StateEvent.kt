package com.kakaouo.viewer.phi.chart.events

import com.kakaouo.viewer.phi.Vector2
import com.kakaouo.viewer.phi.lerp
import com.kakaouo.viewer.phi.model.StateEventModel

class StateEvent : ChartEvent {
    var start = 0.0
    var start2 = 0.0
    var end = 0.0
    var end2 = 0.0

    constructor() : super()
    constructor(other: StateEvent) : super(other) {
        start = other.start
        end = other.end
        start2 = other.start2
        end2 = other.end2
    }

    fun clone(): StateEvent {
        return StateEvent(this)
    }

    companion object {
        fun deserialize(model: StateEventModel): StateEvent {
            val result = StateEvent()
            result.populateFromModel(model)

            result.start = model.start
            result.start2 = model.start2 ?: 0.0
            result.end = model.end
            result.end2 = model.end2 ?: 0.0
            return result
        }
    }

    fun getValueAtTime(time: Double): Double {
        if (duration == 0.0) {
            return (start + end) / 2
        }

        val progress = (time - startTime) / duration
        return lerp(start, end, progress)
    }

    fun getCoordinateAtTime(time: Double): Vector2 {
        if (duration == 0.0) {
            return Vector2((start + end) / 2, (start2 + end2) / 2)
        }

        val progress = (time - startTime) / duration
        val x = lerp(start, end, progress)
        val y = lerp(start2, end2, progress)
        return Vector2(x, y)
    }
}