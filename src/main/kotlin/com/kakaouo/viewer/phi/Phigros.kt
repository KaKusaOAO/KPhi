package com.kakaouo.viewer.phi

import Assets
import com.kakaouo.viewer.phi.Utils.average
import org.w3c.dom.*
import kotlin.math.*

class Phigros(canvas: HTMLCanvasElement): Game(canvas) {
    override fun renderUI() {
        super.renderUI()

        val ctx = context
        val cw = ctx.canvas.width.toDouble()
        val ch = ctx.canvas.height.toDouble()
        val ratio = ratio * 1.25
        val chart = chart
        val audioOffset = config.audioOffset

        var count = 0
        var combo = 0

        val maxValidTime = audioElem.duration * 1000.0

        var beat = 0.0
        if (chart != null) {
            beat = chart.judgeLineList[0].getConvertedGameTime(time) / 32

            chart.judgeLineList.forEach {
                arrayOf(it.notesAbove, it.notesBelow).forEach { arr ->
                    arr.forEach { n ->
                        if (n.cleared) combo++
                        if (it.getRealTimeMillis(n.time) <= maxValidTime) count++
                    }
                }
            }
        }

        if (performance == ChartPerformance.APF_2023) {
            if (beat < 336) {
                count = 305
            } else if (beat < 338) {
                val progress = clamp((beat - 336) / 2, 0.0, 1.0).pow(1 / 3.0)
                count = progress.lerp(305.0, count.toDouble()).toInt()
            }
        }

        var scoreStr = ""
        val maxScore = 1000000
        val score = if (count == 0) 0 else round(maxScore * min(combo, count).toDouble() / count.toDouble()).toInt()
        val digits = log10(maxScore.toDouble()) - 2 - floor(if (score == 0) 0.0 else log10(score.toDouble())) + 1
        for (i in 0 ..digits.toInt()) {
            scoreStr += "0"
        }
        scoreStr += score

        val pad = getRenderXPad()

        // Play bar
        run {
            ctx.fillStyle = "#fff"
            val offset = (chart?.offset ?: 0.0) * 1000 + audioOffset
            var duration = audioElem.duration
            var curr = (time - offset) / 1000
            var audioCurr = audioElem.currentTime

            if (performance == ChartPerformance.APF_2023) {
                if (beat < 336) {
                    duration = 120.0
                } else {
                    val arr = audioAnalyseBuffer
                    val f = arr.average()
                    duration = 255.0
                    audioCurr = f
                    curr = f
                }
            }

            ctx.globalAlpha = 0.3
            ctx.fillRect(pad, 0.0, curr / duration * (canvas.width - pad * 2), 10 * this.ratio)

            ctx.fillStyle = "#fff"
            ctx.fillRect(pad, 0.0, audioCurr / duration * (canvas.width - pad * 2), 10 * this.ratio)

            ctx.globalAlpha = 1.0
            ctx.fillRect(curr / duration * (canvas.width - pad * 2) + pad, 0.0,
                2.5 * ratio + audioCurr / duration * (canvas.width - pad * 2) - (curr / duration * (canvas.width - pad * 2)),
                10 * this.ratio)
        }

        // Pause button
        run {
            ctx.fillStyle = "#000"
            ctx.globalAlpha = 0.5
            ctx.fillRect(30 * ratio + pad, 32 * ratio, 9 * ratio, 29 * ratio)
            ctx.fillRect(47 * ratio + pad, 32 * ratio, 9 * ratio, 29 * ratio)
            ctx.fillStyle = "#fff"
            ctx.globalAlpha = 1.0
            ctx.fillRect(26 * ratio + pad, 28 * ratio, 9 * ratio, 29 * ratio)
            ctx.fillRect(43 * ratio + pad, 28 * ratio, 9 * ratio, 29 * ratio)
        }

        // Song title
        run {
            ctx.fillStyle = "#fff"
            ctx.textAlign = CanvasTextAlign.LEFT
            ctx.font = "${28 * ratio}px ${Assets.preferredFont}"

            val maxTitleWidth = min(545 * ratio, (canvas.width - pad * 2) * 0.6 - 40 * ratio)
            val metrics = ctx.measureText(songName)
            val sScale = if (metrics.width > maxTitleWidth) maxTitleWidth / metrics.width else 1.0
            ctx.font = "${28 * ratio * sScale}px ${Assets.preferredFont}"
            ctx.textBaseline = CanvasTextBaseline.MIDDLE
            ctx.fillText(songName, pad + 40 * ratio, ch - 45 * ratio)
        }

        // Song difficulty & level
        run {
            ctx.font = "${28 * ratio}px ${Assets.preferredFont}"
            ctx.textAlign = CanvasTextAlign.RIGHT

            var text = "${this.diffName} Lv.${if (this.diffLevel < 0) "?" else this.diffLevel.toString()}"
            if (performance == ChartPerformance.APF_2023 && beat < 336) {
                text = "lN Lv.I2"
            }

            ctx.fillText(text, cw - pad - 40 * ratio, ch - 45 * ratio)
        }

        // Combo
        ctx.textBaseline = CanvasTextBaseline.ALPHABETIC
        run {
            if (combo < 3) return@run

            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.font = "500 ${22 * ratio}px ${Assets.preferredFont}"
            ctx.fillText("COMBO", cw / 2, 87 * ratio)

            ctx.font = "500 ${58 * ratio}px ${Assets.preferredFont}"

            var comboStr = combo.toString()
            if (performance == ChartPerformance.APF_2023 && beat >= 650 && beat < 716) {
                val e1 = "(⊙o⊙)"

                comboStr = if (beat < 652) {
                    e1
                } else {
                    val b = (beat - 652) % 16
                    if (b < 2) {
                        e1
                    } else if (b < 14) {
                        "(⊙ω⊙)"
                    } else {
                        "Ｏ(≧▽≦)Ｏ"
                    }
                }
            }

            ctx.fillText(comboStr, cw / 2, 60 * ratio)
        }

        // Apf2023 debug
        if (performance == ChartPerformance.APF_2023 && config.renderDebug) {
            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.font = "500 ${22 * ratio}px ${Assets.preferredFont}"
            ctx.fillText(floor(beat).toString(), cw / 2, 115 * ratio)
        }

        // Score
        run {
            ctx.textAlign = CanvasTextAlign.RIGHT
            ctx.font = "${36 * ratio}px ${Assets.preferredFont}"
            ctx.fillText(scoreStr, cw - pad - 30 * ratio, 55 * ratio)
        }


    }
}