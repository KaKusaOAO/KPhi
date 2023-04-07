package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaTapEvent(time: Int, override val lane: Int) : ArcaeaEvent(time), ILaneNoteEvent {
    companion object {
        fun readLine(line: String): ArcaeaTapEvent {
            val event = line.substring(1, line.length - 2).split(',')
            return ArcaeaTapEvent(
                event[0].toInt(),
                event[1].toInt()
            )
        }
    }
}