package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaHoldEvent(time: Int, override val endTime: Int, override val lane: Int) :
    ArcaeaEvent(time), ILaneNoteEvent, ILongNoteEvent {
    companion object {
        fun readLine(line: String): ArcaeaHoldEvent {
            val event = line.substring("hold(".length, line.length - 2).split(',')
            return ArcaeaHoldEvent(
                event[0].toInt(),
                event[1].toInt(),
                event[2].toInt()
            )
        }
    }
}