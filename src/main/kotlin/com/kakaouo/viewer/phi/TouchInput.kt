package com.kakaouo.viewer.phi

import com.kakaouo.viewer.phi.chart.JudgeLine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TouchInput(val game: Game) {
    var state = 0
    var id = ""
    var xPos = 0.0
    var yPos = 0.0
    var dX = 0.0
    var dY = 0.0

    fun getTransformedPos(line: JudgeLine): Vector2 {
        val x = xPos
        val y = yPos
        val linePos = line.getScaledPosition(line.getLinePosition(game.time))
        linePos.x *= -1
        linePos.y *= -1

        val rad = line.getLineRotation(game.time) / 180.0 * PI
        val c = cos(rad)
        val s = sin(rad)

        val rx = c * x - s * y + (c * linePos.x - s * linePos.y)
        val ry = s * x + c * y + (s * linePos.x + c * linePos.y)
        return Vector2(rx, ry)
    }
}