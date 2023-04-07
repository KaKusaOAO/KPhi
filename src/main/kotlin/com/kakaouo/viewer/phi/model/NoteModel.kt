package com.kakaouo.viewer.phi.model

external interface NoteModel {
    val type: Int
    val time: Double
    val positionX: Double
    val speed: Double
    val floorPosition: Double
    val holdTime: Double?
}