package com.kakaouo.viewer.phi.model

external interface StateEventModel : ChartEventModel {
    val start: Double
    val start2: Double?
    val end: Double
    val end2: Double?
}