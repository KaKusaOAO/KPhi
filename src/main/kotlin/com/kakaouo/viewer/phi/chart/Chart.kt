package com.kakaouo.viewer.phi.chart

import Note
import com.kakaouo.viewer.phi.Game
import com.kakaouo.viewer.phi.model.ChartModel
import kotlin.math.abs

class Chart {
    var game: Game? = null
        private set

    val judgeLineList = arrayListOf<JudgeLine>()
    var offset = 0.0

    val allNotes: List<Note> get() {
        val result = arrayListOf<Note>()
        judgeLineList.forEach {
            result.addAll(it.allNotes)
        }
        return result
    }

    companion object {
        fun deserialize(model: ChartModel): Chart {
            val chart = Chart()
            val formatVersion = model.formatVersion
            chart.offset = model.offset

            chart.judgeLineList.clear()
            chart.judgeLineList.addAll(model.judgeLineList.mapIndexed { i, line ->
                val result = JudgeLine.deserialize(line, formatVersion, i)
                result.parent = chart
                result
            })

            chart.solveSiblings()
            return chart
        }
    }

    fun bindContext(game: Game) {
        if (this.game != null) {
            console.warn("Rebinding context to the chart!")
        }

        this.game = game
    }

    fun solveSiblings() {
        val notes = allNotes

        notes.forEach { a ->
            val t = a.time

            notes.forEach { b ->
                if (abs(t - b.time) < 1 && a != b) {
                    a.hasSibling = true
                    b.hasSibling = true
                }
            }
        }
    }

    fun recalculateFloorPosition() {
        judgeLineList.forEach { it.recalculateFloorPosition() }
    }
}

