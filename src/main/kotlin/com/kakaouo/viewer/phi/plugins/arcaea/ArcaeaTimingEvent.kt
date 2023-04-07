package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaTimingEvent(time: Int, val bpm: Double, val beatsPerLine: Double) : ArcaeaEvent(time) {
    companion object {
        fun readLine(line: String): ArcaeaTimingEvent {
            val event = line.substring("timing(".length, line.length - 2).split(',')
            return ArcaeaTimingEvent(
                event[0].toInt(),
                event[1].toDouble(),
                event[2].toDouble()
            )
        }
    }
}