package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaSceneControlEvent(time: Int, val type: String, val args: List<Double>) : ArcaeaEvent(time) {
    companion object {
        fun readLine(line: String): ArcaeaSceneControlEvent {
            val event = line.substring("scenecontrol(".length, line.length - 2).split(',')
            val time = event[0].toInt()
            val type = event[1]
            val args = event.takeLast(event.size - 2).map { it.toDouble() }

            return ArcaeaSceneControlEvent(time, type, args)
        }
    }

}
