package com.kakaouo.viewer.phi.model

external interface ChartModel {
    val formatVersion: Int
    val offset: Double
    val judgeLineList: Array<JudgeLineModel>
}