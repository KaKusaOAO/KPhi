package com.kakaouo.viewer.phi.chart.events

import com.kakaouo.viewer.phi.model.ChartEventModel
import kotlin.math.max
import kotlin.math.min

open class ChartEvent {
    var startTime = 0.0
    var endTime = 0.0

    val duration get() = endTime - startTime

    constructor()
    constructor(other: ChartEvent) {
        startTime = other.startTime
        endTime = other.endTime
    }

    fun populateFromModel(model: ChartEventModel) {
        startTime = min(model.startTime, model.endTime)
        endTime = max(model.startTime, model.endTime)
    }
}