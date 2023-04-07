package com.kakaouo.viewer.phi.plugins.arcaea

class ArcaeaTimingGroup(val isPrimary: Boolean, args: String = "") {
    val args: List<String>
    val events = arrayListOf<ArcaeaEvent>()

    init {
        this.args = args.split('_')
    }

    val baseBPM get() = events.filterIsInstance<ArcaeaTimingEvent>().first().bpm
    val isNoInput get() = args.contains("noinput")

    companion object {
        fun readLine(line: String): ArcaeaTimingGroup {
            return ArcaeaTimingGroup(false, line.substring("timinggroup(".length, line.indexOf(")")))
        }
    }
}