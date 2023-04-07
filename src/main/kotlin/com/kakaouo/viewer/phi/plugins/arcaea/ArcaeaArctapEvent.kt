package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaArctapEvent(time: Int, val arc: ArcaeaArcEvent) : ArcaeaEvent(time), INoteEvent {
    companion object {
        fun read(n: String, arc: ArcaeaArcEvent): ArcaeaArctapEvent {
            return ArcaeaArctapEvent(
                n.substring("arctap(".length, n.length - 1).toInt(),
                arc
            )
        }
    }
}