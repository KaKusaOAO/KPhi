package com.kakaouo.viewer.phi

import com.kakaouo.viewer.phi.Utils.saveTransform
import kotlinx.browser.window
import kotlin.math.*
import kotlin.random.Random

open class JudgeEffect(val game: Game, x: Double, y: Double, amount: Int) : AnimatedObject() {
    class Particle(val spawnTime: Double, val pos: Vector2, val motion: Vector2)

    var x = x / game.canvas.width * game.config.refScreenWidth
    var y = y / game.canvas.height * game.config.refScreenHeight
    val particles = mutableListOf<Particle>()
    val startTime = window.performance.now()

    init {
        for (i in 0 until amount) {
            val r = Random.nextDouble() * PI * 2
            val force = (Random.nextDouble() * 1 + 1) * 11
            particles.add(
                Particle(
                    startTime,
                    Vector2(0.0, 0.0),
                    Vector2(cos(r) * force, sin(r) * force)
                )
            )
        }
    }

    override fun fixedUpdate() {
        super.fixedUpdate()
        particles.forEach {
            it.pos += it.motion
            it.motion *= (0.92).pow(game.audioElem.playbackRate.pow(0.275))
        }
    }

    override fun update() {
        super.update()
        val progress = (window.performance.now() - startTime) / (500 / game.audioElem.playbackRate)
        if (progress >= 1) {
            notNeeded = true
        }
    }

    override fun render() {
        super.render()
        val ctx = game.context
        val color = "#fea"
        val size = 100 * game.getNoteRatio()
        val progress = (window.performance.now() - startTime) / (500 / game.audioElem.playbackRate)

        ctx.saveTransform {
            ctx.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            ctx.translate(x * game.canvas.width / game.config.refScreenWidth, y * game.canvas.height / game.config.refScreenHeight)

            val pInv = max(0.0, 1 - progress)
            ctx.strokeStyle = color
            ctx.fillStyle = color
            ctx.lineWidth = 4 * game.getNoteRatio()
            ctx.globalAlpha = pInv.pow(1.1)

            val s2 = size * (0.75 + 0.25 * (1 - pInv.pow(5)))
            ctx.strokeRect(-s2, -s2, s2 * 2, s2 * 2)
            ctx.rotate(PI / 4)

            val sThick = 48 * pInv.pow(2)
            ctx.lineWidth = (sThick * pInv.pow(2)) * game.getNoteRatio()

            val s3 = size - sThick * 0.5 * pInv.pow(2) * game.getNoteRatio() * (0.8 + 0.3 * progress.pow(0.25))
            val aa = pInv.pow(2)
            ctx.globalAlpha *= 0.125
            ctx.strokeRect(-s3, -s3, s3 * 2, s3 * 2)
            ctx.rotate(-PI / 4)

            ctx.lineWidth = 6 * game.getNoteRatio()
            ctx.globalAlpha = aa.pow(0.33)

            val offset = -pInv.pow(3) * (PI / 4)
            ctx.beginPath()
            ctx.arc(0.0, 0.0, s2 * 0.9, offset, offset - PI / 2, true)
            ctx.stroke()
            ctx.beginPath()
            ctx.arc(0.0, 0.0, s2 * 0.9, offset + PI / 2, offset + PI)
            ctx.stroke()
            ctx.globalAlpha = aa

            ctx.beginPath()
            ctx.arc(0.0, 0.0, size * min(0.25, 0.1 / max(0.0,1 - pInv.pow(4))), 0.0, PI * 2)
            ctx.fill()
            ctx.globalAlpha /= 4

            ctx.beginPath()
            ctx.arc(0.0, 0.0, size * 0.5 * progress.pow(0.25), 0.0, PI * 2)
            ctx.fill()
            ctx.globalAlpha *= 4

            // Particles
            particles.forEach {
                val x = it.pos.x * game.getNoteRatio()
                val y = it.pos.y * game.getNoteRatio()
                val ps = (progress.pow(0.25) + 1) * 7.5 * game.getNoteRatio()
                ctx.fillRect(-ps + x, -ps + y, ps * 2, ps * 2)
            }
        }

        ctx.lineWidth = 0.0
        ctx.globalAlpha = 1.0
    }
}