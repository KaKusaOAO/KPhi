package com.kakaouo.viewer.phi.chart

import HoldNote
import Note
import com.kakaouo.viewer.phi.Utils.findEventOrFirst
import com.kakaouo.viewer.phi.Utils.saveTransform
import com.kakaouo.viewer.phi.Vector2
import com.kakaouo.viewer.phi.chart.events.SpeedEvent
import com.kakaouo.viewer.phi.chart.events.StateEvent
import com.kakaouo.viewer.phi.model.JudgeLineModel
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.DOMMatrix
import org.w3c.dom.Image
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class JudgeLine {
    var index = -1
    var bpm = 120.0
    var parent: Chart? = null
    val game get() = parent?.game

    val notesAbove = arrayListOf<Note>()
    val notesBelow = arrayListOf<Note>()
    val speedEvents = arrayListOf<SpeedEvent>()
    val judgeLineDisappearEvents = arrayListOf<StateEvent>()
    val judgeLineRotateEvents = arrayListOf<StateEvent>()
    val judgeLineMoveEvents = arrayListOf<StateEvent>()

    var texture: Image? = null
    var texturePos = Vector2(1.0, 1.0)

    val allNotes: List<Note> get() {
        val result = arrayListOf<Note>()
        result.addAll(notesAbove)
        result.addAll(notesBelow)
        return result
    }

    fun getScaledPosition(pos: Vector2): Vector2 {
        var x = pos.x
        var y = pos.y
        val game = game!!
        val pad = game.getRenderXPad()
        val cw = game.canvas.width.toDouble() - pad * 2
        val ch = game.canvas.height.toDouble()

        x = 0.5 * cw + (x - 0.5) * cw + pad
        y = ch - 0.5 * ch - (y - 0.5) * ch
        return Vector2(x, y)
    }

    fun getRotatedPosition(pos: Vector2 = getLinePosition(game!!.time),
                           angle: Double = getLineRotation(game!!.time)): Vector2 {
        val rad = angle / 180.0 * PI
        val c = cos(rad)
        val s = sin(rad)

        val x = c * pos.x - s * pos.y
        val y = s * pos.x + c * pos.y
        return Vector2(x, y)
    }

    fun update() {
        notesAbove.forEach { it.update() }
        notesBelow.forEach { it.update() }
    }

    var transform = DOMMatrix(arrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0))
    val cachedLinePos = mutableMapOf<Double, Vector2>()
    val cachedLineRot = mutableMapOf<Double, Double>()
    val cachedLineAlpha = mutableMapOf<Double, Double>()
    val cachedFloorPosition = mutableMapOf<Double, Double>()

    fun renderNotes() {
        val game = game!!
        val ctx = game.context
        val cw = ctx.canvas.width.toDouble() - game.getRenderXPad() * 2
        val ch = ctx.canvas.height.toDouble()
        val time = game.time

        cachedLinePos.clear()
        cachedLineRot.clear()
        cachedLineAlpha.clear()
        cachedFloorPosition.clear()

        ctx.saveTransform {
            val linePos = getScaledPosition(getLinePosition(time))
            val lineRot = -getLineRotation(time) / 180 * PI
            ctx.translate(linePos.x, linePos.y)
            ctx.rotate(lineRot)

            fun renderNote(note: Note) {
                if (note.isOffscreen() && !game.config.offScreenForceRender) return
                val doClip = note is HoldNote

                if (game.config.renderDebug) {
                    note.renderTouchArea()
                }

                if (doClip) {
                    ctx.save()
                    ctx.beginPath()
                    ctx.rect(-cw, -ch * 2, cw * 2, ch * 2)
                    ctx.clip()
                }

                note.render()

                if (doClip) {
                    ctx.restore()
                }
            }

            notesAbove.filterIsInstance<HoldNote>().forEach { renderNote(it) }
            notesAbove.filter { it !is HoldNote }.forEach { renderNote(it) }
            ctx.scale(1.0, -1.0)

            notesBelow.filterIsInstance<HoldNote>().forEach { renderNote(it) }
            notesBelow.filter { it !is HoldNote }.forEach { renderNote(it) }
            ctx.scale(1.0, -1.0)

            transform = ctx.getTransform()
        }
    }

    fun renderLine() {
        val game = game!!
        val ctx = game.context
        val cw = ctx.canvas.width.toDouble() - game.getRenderXPad() * 2
        val ch = ctx.canvas.height.toDouble()
        val time = game.time
        val ratio = game.ratio

        ctx.saveTransform {
            ctx.setTransform(transform)
            ctx.globalAlpha = getLineAlpha(time)
            ctx.fillStyle = "#fff"

            if (game.config.renderDebug) {
                ctx.beginPath()
                ctx.arc(0.0, 0.0, 20 * game.ratio, 0.0, 2 * PI)
                ctx.fill()
            }

            val img = texture
            if (img == null) {
                val thickness = 8 * game.getNoteRatio()
                ctx.fillRect(-cw * 2, thickness / -2, cw * 4, thickness)
            } else {
                val unit = 900.0
                val iw = img.width / unit * ch
                val ih = img.height / unit * ch
                val xOffset = ih * (texturePos.x - 1) / 4
                val yOffset = ih * (texturePos.y - 1) / 4

                ctx.drawImage(img, -iw / 2 - xOffset, -ih / 2 - yOffset, iw, ih)
            }

            if (game.config.renderDebug) {
                ctx.textAlign = CanvasTextAlign.CENTER
                ctx.font = "${28 * game.ratio}px ${Assets.preferredFont}"

                if (notesAbove.size + notesBelow.size > 0) {
                    ctx.fillText("[${index}] bpm=${bpm} " +
                            "t=${floor(getConvertedGameTime(game.time) / 32)} f=${getCurrentFloorPosition()}",
                        0.0, -24 * ratio);
                } else {
                    ctx.fillText("[${index}] bpm=${bpm}", 0.0, -24 * ratio)
                }
            }

            ctx.fillStyle = "#000"
            ctx.globalAlpha = 1.0
        }
    }

    fun getLineAlpha(time: Double): Double {
        if (cachedLineAlpha.containsKey(time)) {
            return cachedLineAlpha[time]!!
        }

        val gt = getConvertedGameTime(time)
        val ev = judgeLineDisappearEvents.findEventOrFirst(gt)
        if (ev == null) {
            cachedLineAlpha[time] = 1.0
            return 1.0
        }

        val result = ev.getValueAtTime(gt)
        cachedLineAlpha[time] = result
        return result
    }

    fun getConvertedGameTime(millis: Double): Double {
        return millis * (bpm / 1875)
    }

    fun getRealTimeMillis(time: Double): Double {
        return time * 1875 / bpm
    }

    fun getLinePosition(time: Double): Vector2 {
        if (cachedLinePos.containsKey(time)) {
            return cachedLinePos[time]!!
        }

        val gt = getConvertedGameTime(time)
        val ev = judgeLineMoveEvents.findEventOrFirst(gt)
        if (ev == null) {
            val r = Vector2(0.5, 0.5)
            cachedLinePos[time] = r
            return r
        }

        val result = ev.getCoordinateAtTime(gt)
        cachedLinePos[time] = result
        return result
    }

    fun getLineRotation(time: Double): Double {
        if (cachedLineRot.containsKey(time)) {
            return cachedLineRot[time]!!
        }

        val gt = getConvertedGameTime(time)
        val ev = judgeLineRotateEvents.findEventOrFirst(gt)
        if (ev == null) {
            cachedLineRot[time] = 0.0
            return 0.0
        }

        val result = ev.getValueAtTime(gt)
        cachedLineRot[time] = result
        return result
    }

    fun getCurrentFloorPosition(): Double {
        val time = getConvertedGameTime(game!!.time)
        return getCalculatedFloorPosition(time)
    }

    fun getCalculatedFloorPosition(time: Double): Double {
        if (cachedFloorPosition.containsKey(time)) {
            return cachedFloorPosition[time]!!
        }
        val ev = speedEvents.findEventOrFirst(time) ?: return 0.0
        val result = (ev.floorPosition ?: 0.0) + getRealTimeMillis(time - ev.startTime) / 1000 * ev.value
        cachedFloorPosition[time] = result
        return result
    }

    fun recalculateSpeedEventsFloorPosition() {
        var posY = 0.0
        speedEvents.forEach {
            it.floorPosition = posY
            posY += it.value * it.duration / bpm * 1.875
        }
    }

    fun recalculateNotesFloorPosition() {
        arrayOf(notesAbove, notesBelow).forEach {
            it.forEach inner@ { n ->
                val time = n.time
                val ev = speedEvents.findEventOrFirst(time) ?: return@inner
                n.floorPosition = ev.floorPosition!! + getRealTimeMillis(time - ev.startTime) / 1000 * ev.value
            }
        }
    }

    fun recalculateFloorPosition() {
        recalculateSpeedEventsFloorPosition()
        recalculateNotesFloorPosition()
    }

    fun getYPosByFloorPos(floor: Double): Double {
        return floor * 0.6 * game!!.canvas.height
    }

    fun getYPosition(time: Double): Double {
        return getYPosByFloorPos(getCalculatedFloorPosition(time))
    }

    fun getYPosWithGame(time: Double): Double {
        val gt = getConvertedGameTime(time)
        return getYPosition(time) - getYPosition(gt)
    }

    companion object {
        fun deserialize(model: JudgeLineModel, formatVersion: Int, index: Int): JudgeLine {
            val line = JudgeLine()
            line.index = index
            line.bpm = model.bpm

            line.notesAbove.clear()
            line.notesAbove.addAll(model.notesAbove.map {
                val note = Note.deserialize(it)
                note.parent = line
                note
            })

            line.notesBelow.clear()
            line.notesBelow.addAll(model.notesBelow.map {
                val note = Note.deserialize(it)
                note.parent = line
                note
            })

            var posY = 0.0
            line.speedEvents.clear()
            line.speedEvents.addAll(model.speedEvents.map {
                val ev = SpeedEvent.deserialize(it)

                if (ev.floorPosition == null) {
                    if (formatVersion >= 3) {
                        console.warn("Found a speed event without a floorPosition value! Calculating floor position...")
                    }

                    ev.floorPosition = posY
                    posY += ev.value * ev.duration / line.bpm * 1.875
                }

                ev
            })

            line.judgeLineDisappearEvents.clear()
            line.judgeLineDisappearEvents.addAll(model.judgeLineDisappearEvents.map {
                StateEvent.deserialize(it)
            })

            line.judgeLineRotateEvents.clear()
            line.judgeLineRotateEvents.addAll(model.judgeLineRotateEvents.map {
                StateEvent.deserialize(it)
            })

            line.judgeLineMoveEvents.clear()
            line.judgeLineMoveEvents.addAll(model.judgeLineMoveEvents.map {
                val ev = StateEvent.deserialize(it)
                if (formatVersion == 3) return@map ev

                val xCenter = 440
                val yCenter = 260

                val startX = floor(ev.start / 1000)
                val startY = floor(ev.start % 1000)
                val endX = floor(ev.end / 1000)
                val endY = floor(ev.end % 1000)

                ev.start = startX / xCenter / 2
                ev.start2 = startY / yCenter / 2
                ev.end = endX / xCenter / 2
                ev.end2 = endY / yCenter / 2

                ev
            })

            line.judgeLineDisappearEvents.sortBy { it.startTime }
            line.judgeLineRotateEvents.sortBy { it.startTime }
            line.judgeLineMoveEvents.sortBy { it.startTime }

            return line
        }
    }
}