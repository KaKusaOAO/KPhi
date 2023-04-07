import com.kakaouo.viewer.phi.model.NoteModel
import kotlinx.browser.window
import kotlin.math.round

class HoldNote : Note(NoteType.HOLD) {
    var holdTime = 0.0

    val endTime get() = time + holdTime
    var lastJudge = 0.0

    companion object {
        fun deserialize(model: NoteModel): HoldNote {
            val result = HoldNote()
            result.populateFromModel(model)
            result.holdTime = model.holdTime!!
            return result
        }
    }

    override fun isNegativeFloor(): Boolean {
        return false
    }

    override fun isMissed(): Boolean {
        // Temp workaround
        return false
    }

    override fun update() {
        super.update()

        val game = game!!
        val parent = parent!!
        val gt = parent.getConvertedGameTime(game.time)

        if (crossed && gt < endTime) {
            val now = window.performance.now()
            if (now - lastJudge > 75) {
                lastJudge = now
                spawnJudge()
            }
        } else {
            lastJudge = 0.0
        }
    }

    override fun render() {
        super.render()
        val game = game!!
        val parent = parent!!
        if (isOffscreen() && !game.config.offScreenForceRender) return

        val ctx = game.context
        val ratio = game.getNoteRatio()
        val yPos = round(getYPos())
        val xPos = getXPos()

        var w = round(noteWidth * ratio)
        val h = round(parent.getYPosWithGame(time + holdTime) - parent.getYPosWithGame(time))

        val gt = parent.getConvertedGameTime(game.time)
        if (gt > time + holdTime) return

        val headTexture = game.assets.image(if (hasSibling) "holdHLHead" else "holdHead")!!
        val headH = round(headTexture.height.toDouble() / headTexture.width * w)
        val endTexture = game.assets.image("holdEnd")!!
        var endH = round(endTexture.height.toDouble() / endTexture.width * w)
        var tx = round(-w / 2 + xPos)
        ctx.drawImage(endTexture, tx, -yPos - h, w, endH)

        if (hasSibling) {
            w *= 1060.0 / 989 * 1.025
            w = round(w)
            endH -= round(ratio * 1060.0 / 989)
            tx = round(-w / 2 + xPos)
        }

        val texture = game.assets.image(if (hasSibling) "holdHL" else "hold")!!
        ctx.drawImage(texture, tx, round(-yPos - h + endH + 0.501), w, round(h - endH - headH - 0.501))
        ctx.drawImage(headTexture, tx, -yPos - headH, w, headH)
    }
}