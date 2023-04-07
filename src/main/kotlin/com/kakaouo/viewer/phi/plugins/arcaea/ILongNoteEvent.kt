package com.kakaouo.viewer.phi.plugins.arcaea

interface ILongNoteEvent : INoteEvent {
    val endTime: Int

    val duration get() = endTime - time
}