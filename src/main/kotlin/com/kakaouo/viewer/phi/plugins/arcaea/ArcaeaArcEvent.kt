package com.kakaouo.viewer.phi.plugins.arcaea

import com.kakaouo.viewer.phi.Vector2

class ArcaeaArcEvent(time: Int,
                     override val endTime: Int,
                     xStart: Double,
                     xEnd: Double,
                     val lineType: LineEasingType,
                     yStart: Double,
                     yEnd: Double,
                     val color: ArcColor,
                     val isVoid: Boolean) : ArcaeaEvent(time), ILongNoteEvent {
    val start = Vector2(xStart, yStart)
    val end = Vector2(xEnd, yEnd)
    val arcTaps = arrayListOf<ArcaeaArctapEvent>()

    fun interpolate(progress: Double): Vector2 {
        return lineType.interpolate(start, end, progress)
    }

    fun interpolateX(progress: Double): Double {
        return lineType.interpolateX(start.x, end.x, progress)
    }

    fun interpolateY(progress: Double): Double {
        return lineType.interpolateX(start.y, end.y, progress)
    }

    companion object {
        fun readLine(line: String): ArcaeaArcEvent {
            if (!line.startsWith("arc(")) {
                throw Error("The given line is not an arc event.")
            }

            val regex = Regex("arc\\((\\d+?),(\\d+?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)\\)(?:\\[(.*?)\\])?;")
            val event = regex.find(line)!!

            val arc = ArcaeaArcEvent(
                event.groupValues[1].toInt(),
                event.groupValues[2].toInt(),
                event.groupValues[3].toDouble(),
                event.groupValues[4].toDouble(),
                LineEasingType.of(event.groupValues[5]),
                event.groupValues[6].toDouble(),
                event.groupValues[7].toDouble(),
                ArcColor.values()[event.groupValues[8].toInt()],
                event.groupValues[10] == "true"
            )

            val arctaps = event.groupValues[11]
            if (arctaps.isNotEmpty()) {
                val t = arctaps.split(',')
                for (n in t) {
                    arc.arcTaps.add(ArcaeaArctapEvent.read(n, arc))
                }
            }

            return arc
        }
    }
}

