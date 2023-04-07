package com.kakaouo.viewer.phi.chart.events

import com.kakaouo.viewer.phi.model.SpeedEventModel

class SpeedEvent : ChartEvent {
    var value = 0.0
    var floorPosition: Double? = 0.0

    constructor()
    constructor(other: SpeedEvent) : super(other) {
        value = other.value
        floorPosition = other.floorPosition
    }

    fun clone(): SpeedEvent {
        return SpeedEvent(this)
    }

    companion object {
        fun deserialize(model: SpeedEventModel): SpeedEvent {
            val result = SpeedEvent()
            result.populateFromModel(model)

            result.value = model.value
            result.floorPosition = model.floorPosition
            return result
        }
    }
}