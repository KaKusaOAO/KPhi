package com.kakaouo.viewer.phi.plugins.arcaea

import CatchNote
import FlickNote
import HoldNote
import TapNote
import com.kakaouo.viewer.phi.Utils.findEventOrFirst
import com.kakaouo.viewer.phi.chart.Chart
import com.kakaouo.viewer.phi.chart.JudgeLine
import com.kakaouo.viewer.phi.chart.events.SpeedEvent
import com.kakaouo.viewer.phi.chart.events.StateEvent
import com.kakaouo.viewer.phi.clamp
import com.kakaouo.viewer.phi.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ArcaeaChartConverter {
    private class SpeedEventRecord(val time: Double, var endTime: Double, val speed: Double, val beatsPerLine: Double)

    private fun mapNormalizedXToNotePosX(n: Double): Double {
        return n.lerp(-1.0, 1.0) * 4
    }

    private fun mapNormalizedXToLineX(n: Double): Double {
        return n.lerp(0.275, 0.725)
    }

    private fun laneToX(lane: Int): Double {
        return mapNormalizedXToNotePosX((lane.toDouble() - 1) / 3)
    }

    private fun normalizeArcX(x: Double): Double {
        // Map arc X positions to [0, 1] range
        return (x + 0.25) / 2 / 3 * 4
    }

    private fun arcXToPhigros(x: Double): Double {
        return mapNormalizedXToNotePosX(normalizeArcX(x))
    }

    private fun arcXToPhigrosLine(x: Double) : Double {
        return mapNormalizedXToLineX(normalizeArcX(x))
    }

    private fun arcYToPhigrosAlpha(y: Double): Double {
        return y.clamp(0.0, 1.0).lerp(0.125, 0.5)
    }

    private fun arcTimeToPhigros(source: Int, bpm: Double): Double {
        return arcTimeToPhigros(source.toDouble(), bpm)
    }

    private fun arcTimeToPhigros(source: Double, bpm: Double): Double {
        return source / 1875 * bpm
    }

    fun convert(arcaea: ArcaeaChart, arcDensity: Double = 1.0, speedMultiplier: Double = 1.0): Chart {
        console.log("Loading from Arcaea chart: ", arcaea)

        val chart = Chart().apply {
            offset = arcaea.offset * -0.001
        }

        val baseBPM = arcaea.primaryGroup.baseBPM
        val noInputLines = arrayListOf<JudgeLine>()

        arcaea.timingGroups.forEach { g ->
            g.events.sortBy { it.time }

            val arcLine = JudgeLine().apply {
                bpm = baseBPM
            }
            val notesLine = JudgeLine().apply {
                bpm = baseBPM
            }

            if (g.isNoInput) {
                noInputLines.add(arcLine)
                noInputLines.add(notesLine)
            }

            val s1 = StateEvent().apply {
                startTime = 0.0
                endTime = 99999999.0
                start = 0.5
                end = 0.5
                start2 = 0.15
                end2 = 0.15
            }
            val a1 = StateEvent().apply {
                startTime = 0.0
                endTime = 99999999.0
                start = 1.0
                end = 1.0
            }

            arcLine.judgeLineMoveEvents.add(s1.clone())
            notesLine.judgeLineMoveEvents.add(s1.clone())
            arcLine.judgeLineDisappearEvents.add(a1)
            notesLine.judgeLineDisappearEvents.add(a1)

            chart.judgeLineList.add(arcLine)
            chart.judgeLineList.add(notesLine)

            val speeds = arrayListOf<SpeedEventRecord>().apply {
                add(SpeedEventRecord(-1.0, 99999999.0, 1.0, 4.0))
            }
            var maxTime = 0.0

            g.events.filterIsInstance<ArcaeaTimingEvent>().forEach { event ->
                val speed = event.bpm / baseBPM
                val time = arcTimeToPhigros(event.time, baseBPM)
                speeds.last().endTime = time
                speeds.add(SpeedEventRecord(
                    time, 99999999.0, speed, event.beatsPerLine
                ))
                maxTime = max(maxTime, time)
            }

            speeds.sortBy { it.time }
            speeds.forEachIndexed { i, s ->
                if (i == 0) return@forEachIndexed

                val event = SpeedEvent().apply {
                    startTime = s.time
                    endTime = s.endTime
                    value = s.speed * 1.25 * speedMultiplier
                }

                arcLine.speedEvents.add(event.clone())
                notesLine.speedEvents.add(event.clone())
            }

            arcLine.recalculateSpeedEventsFloorPosition()
            notesLine.recalculateSpeedEventsFloorPosition()

            fun floorPositionToLinePosY(time: Double): Double {
                return notesLine.getCalculatedFloorPosition(time) * 0.6
            }

            fun getSpeed(time: Double): Double {
                val event = notesLine.speedEvents.findEventOrFirst(time)
                return event?.value ?: speedMultiplier
            }

            val voidLines = arrayListOf<JudgeLine>()
            g.events.forEach { event ->
                val pTime = arcTimeToPhigros(event.time, baseBPM)
                if (event is ArcaeaTapEvent) {
                    TapNote().apply {
                        time = pTime
                        maxTime = max(maxTime, time)

                        positionX = laneToX(event.lane)
                        parent = notesLine
                        notesLine.notesAbove.add(this)
                    }
                } else if (event is ArcaeaHoldEvent) {
                    HoldNote().apply {
                        time = pTime
                        holdTime = arcTimeToPhigros(event.duration, baseBPM)
                        maxTime = max(maxTime, endTime)

                        positionX = laneToX(event.lane)
                        parent = notesLine
                        speed = getSpeed(time)
                        notesLine.notesAbove.add(this)
                    }
                } else if (event is ArcaeaSceneControlEvent) {
                    if (event.type == "hidegroup") {
                        val hidden = event.args[1] == 0.0
                        val y0 = if (hidden) 2.0 else 0.15
                        val y = if (!hidden) 2.0 else 0.15
                        val x0 = 0.5
                        val x = 0.5

                        val ev = StateEvent().apply {
                            startTime = pTime
                            endTime = pTime + 0.01
                            start = x0
                            end = x
                            start2 = y0
                            end2 = y
                        }
                        val ev2 = StateEvent().apply {
                            startTime = ev.endTime
                            start = x
                            end = x
                            start2 = y
                            end2 = y
                        }

                        val l = notesLine.judgeLineMoveEvents.last()
                        ev2.endTime = l.endTime

                        l.endTime = ev.startTime
                        arcLine.judgeLineMoveEvents.last().endTime = ev.startTime

                        notesLine.judgeLineMoveEvents.add(ev)
                        notesLine.judgeLineMoveEvents.add(ev2)
                        arcLine.judgeLineMoveEvents.add(ev)
                        arcLine.judgeLineMoveEvents.add(ev2)
                    }
                } else if (event is ArcaeaArcEvent) {
                    val aTime = pTime
                    val bTime = arcTimeToPhigros(event.endTime, baseBPM)

                    if (!event.isVoid) {
                        var density: Double

                        fun getArcSpeed(time: Double): Double {
                            val ev = notesLine.speedEvents.findEventOrFirst(time) ?: return 1.0
                            return ev.value / speedMultiplier / 1.25
                        }

                        var i = aTime
                        while (i < bTime) {
                            density = abs(arcDensity * getArcSpeed(i))
                            CatchNote().apply {
                                time = i
                                maxTime = max(maxTime, i)

                                val progress = (i - aTime) / (bTime - aTime)
                                positionX = arcXToPhigros(event.interpolateX(progress))
                                parent = arcLine
                                arcLine.notesAbove.add(this)
                            }

                            i += density
                        }
                    } else {
                        var voidLine: JudgeLine? = null

                        // Avoid creating a new line for *every* void arcs!
                        // Reuse old ones when applicable.
                        for (line in voidLines) {
                            val lastEvent = line.judgeLineDisappearEvents.lastOrNull()
                            if (lastEvent != null && lastEvent.startTime <= aTime) {
                                voidLine = line
                                break
                            }
                        }

                        if (voidLine == null) {
                            voidLine = JudgeLine().apply {
                                bpm = baseBPM

                                StateEvent().apply {
                                    startTime = -99999999.0
                                    endTime = aTime
                                    start = 0.0
                                    end = 0.0
                                    judgeLineDisappearEvents.add(this)
                                }
                            }
                            voidLines.add(voidLine)
                        } else {
                            val lastEvent = voidLine.judgeLineDisappearEvents.last()
                            lastEvent.endTime = aTime
                        }

                        var i = aTime
                        while (i < bTime) {
                            val pA = (i - aTime) / (bTime - aTime)
                            val et = min(i + 1, bTime)
                            val pB = (et - aTime) / (bTime - aTime)

                            val s = StateEvent().apply {
                                startTime = i
                                endTime = et
                                start = arcXToPhigrosLine(event.interpolateX(pA))
                                end = arcXToPhigrosLine(event.interpolateX(pB))
                                start2 = 0.15
                                end2 = 0.15
                            }
                            voidLine.judgeLineMoveEvents.add(s)

                            val r = StateEvent().apply {
                                startTime = i
                                endTime = et
                                start = 90 - (s.end - s.start) * 180
                                end = start
                            }
                            voidLine.judgeLineRotateEvents.add(r)

                            val a2 = StateEvent().apply {
                                startTime = i
                                endTime = et
                                start = arcYToPhigrosAlpha(event.interpolateY(pA))
                                end = arcYToPhigrosAlpha(event.interpolateY(pB))
                            }
                            voidLine.judgeLineDisappearEvents.add(a2)

                            i++
                        }

                        val a3 = StateEvent().apply {
                            startTime = bTime
                            endTime = 99999999.0
                            end = 0.0
                            start = end
                        }
                        voidLine.judgeLineDisappearEvents.add(a3)

                        // Arctaps
                        event.arcTaps.forEach { t ->
                            val tTime = arcTimeToPhigros(t.time, baseBPM)
                            val progress = (tTime - aTime) / (bTime - aTime)
                            FlickNote().apply {
                                time = tTime
                                maxTime = max(maxTime, time)

                                positionX = arcXToPhigros(event.interpolateX(progress))
                                parent = arcLine
                                arcLine.notesAbove.add(this)
                            }
                        }
                    }
                }
            }

            // Beatlines
            val beatLines = arrayListOf<JudgeLine>()
            speeds.forEachIndexed { i, ev ->
                if (i == 0) return@forEachIndexed
                val isLast = i == speeds.size - 1

                if (g.isPrimary) {
                    val bpm = notesLine.bpm
                    val realBpm = abs(ev.speed) * baseBPM
                    val timePerLine = arcTimeToPhigros(60000.0 / realBpm * ev.beatsPerLine, baseBPM)

                    var t = ev.time
                    while (t < (if (isLast) (maxTime + 32 * 4) else ev.endTime) - 1) {
                        val line = JudgeLine().apply {
                            this.bpm = bpm
                        }

                        val a2 = StateEvent().apply {
                            endTime = t
                            end = 0.5
                            start = 0.5
                        }
                        val a3 = StateEvent().apply {
                            startTime = t
                            endTime = 99999999.0
                        }
                        line.judgeLineDisappearEvents.add(a2)
                        line.judgeLineDisappearEvents.add(a3)

                        for (spev in notesLine.speedEvents) {
                            val et = min(spev.endTime, t)

                            val currPos0 = floorPositionToLinePosY(spev.startTime)
                            val currPos = floorPositionToLinePosY(et)
                            val linePos = floorPositionToLinePosY(t)

                            val s = StateEvent().apply {
                                startTime = spev.startTime
                                endTime = et
                                start = 0.5
                                end = 0.5
                                start2 = linePos - currPos0 + 0.15
                                end2 = linePos - currPos + 0.15
                            }
                            line.judgeLineMoveEvents.add(s)

                            if (et == t) break
                        }

                        beatLines.add(line)
                        t += timePerLine
                    }
                }
            }

            // Merge the beatlines
            var i = 1
            while (i < beatLines.size) {
                val line = beatLines[i]

                try {
                    val target = beatLines.filterIndexed { j, l ->
                        if (j > i) return@filterIndexed false
                        val lastEvent = l.judgeLineDisappearEvents.lastOrNull() ?: return@filterIndexed false

                        // The time it disappears, that time `line` is out of range
                        val dt = l.getRealTimeMillis(lastEvent.startTime)
                        val y = line.getLinePosition(dt).y
                        return@filterIndexed (y > 1 || y < 0)
                    }.firstOrNull() ?: continue

                    val de = target.judgeLineDisappearEvents
                    val dl = de.takeLast(2)

                    val del = line.judgeLineDisappearEvents
                    val dell = del.last()

                    dl[1].startTime = dell.startTime
                    dl[0].endTime = dell.startTime

                    // Position
                    val pe = target.judgeLineMoveEvents
                    val pel = pe.last()

                    val lpe = line.judgeLineMoveEvents
                    val et = pel.endTime
                    val lpel = lpe.filter { it.endTime >= et }
                    if (lpel.isNotEmpty()) {
                        lpel[0].apply {
                            start2 = line.getLinePosition(target.getRealTimeMillis(et)).y
                            startTime = et
                        }
                        pe.addAll(lpel)
                    } else {
                        console.warn("lpel is empty??")
                    }

                    beatLines.removeAt(i)
                    i--
                } finally {
                    i++
                }
            }

            chart.judgeLineList.addAll(voidLines)
            chart.judgeLineList.addAll(beatLines)
            console.log("Beatlines:", beatLines)
        }

        chart.solveSiblings()

        val maxTime = chart.judgeLineList.flatMap { l ->
            arrayOf(l.notesAbove, l.notesBelow).flatMap { arr ->
                arr.map { it.time }
            }
        }.max()

        chart.judgeLineList.forEachIndexed { i, line ->
            line.index = i
            line.judgeLineDisappearEvents.sortBy { it.startTime }
            line.judgeLineRotateEvents.sortBy { it.startTime }
            line.judgeLineMoveEvents.sortBy { it.startTime }
        }

        // Handle no-input lines
        noInputLines.forEach { l ->
            l.recalculateNotesFloorPosition()

            val clone = l.speedEvents.map { it.clone() }
            clone.forEach {
                it.startTime += maxTime + 1
                it.endTime += maxTime + 1
            }

            val lsp = l.speedEvents.last()
            lsp.endTime = maxTime

            val sp2 = SpeedEvent().apply {
                startTime = maxTime
                endTime = maxTime + 1
                value = 1.0
            }

            l.speedEvents.add(sp2)
            l.speedEvents.addAll(clone)
            l.recalculateSpeedEventsFloorPosition()

            // Make the next floorPosition become 0
            val spf = sp2.floorPosition!!
            sp2.value = -spf / 1.875 * l.bpm
            l.recalculateSpeedEventsFloorPosition()

            val sp = l.speedEvents.last()
            val notes = l.allNotes
            notes.forEach { n ->
                val f = n.floorPosition
                val t = ((f - sp.floorPosition!!) / sp.value * 1000 * l.bpm / 1875) + sp.startTime
                n.time = t
            }

            sp.endTime = 99999999.0
        }

        chart.recalculateFloorPosition()
        chart.judgeLineList.forEach {
            it.parent = chart
        }
        return chart
    }

    fun Chart.Companion.fromArcaea(arcaea: ArcaeaChart, arcDensity: Double = 1.0, speedMultiplier: Double = 1.0): Chart {
        return convert(arcaea, arcDensity, speedMultiplier)
    }
}