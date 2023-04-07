package com.kakaouo.viewer.phi.model

external interface JudgeLineModel {
    val bpm: Double
    val notesAbove: Array<NoteModel>
    val notesBelow: Array<NoteModel>
    val speedEvents: Array<SpeedEventModel>
    val judgeLineDisappearEvents: Array<StateEventModel>
    val judgeLineRotateEvents: Array<StateEventModel>
    val judgeLineMoveEvents: Array<StateEventModel>
}