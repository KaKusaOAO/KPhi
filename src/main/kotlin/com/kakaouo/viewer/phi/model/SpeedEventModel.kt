package com.kakaouo.viewer.phi.model

external interface SpeedEventModel : ChartEventModel {
    val value: Double
    val floorPosition: Double?
}