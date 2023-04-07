import com.kakaouo.viewer.phi.Vector2
import com.kakaouo.viewer.phi.JudgeEffect
import com.kakaouo.viewer.phi.chart.JudgeLine
import com.kakaouo.viewer.phi.clamp
import com.kakaouo.viewer.phi.model.NoteModel
import com.kakaouo.viewer.phi.polyfill.audio.AudioContextState
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.Image
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

open class Note(val type: NoteType) {
    var time = 0.0
    var positionX = 0.0
    var speed = 1.0
    var floorPosition = 0.0
    var parent: JudgeLine? = null

    val chart get() = parent?.parent
    val game get() = chart?.game
    var cleared = false
    var crossed = false
    var hasSibling = false
    var noteWidth = 240.0

    companion object {
        fun deserialize(model: NoteModel): Note {
            return when (model.type) {
                NoteType.TAP.ordinal -> TapNote.deserialize(model)
                NoteType.FLICK.ordinal -> FlickNote.deserialize(model)
                NoteType.HOLD.ordinal -> HoldNote.deserialize(model)
                NoteType.CATCH.ordinal -> CatchNote.deserialize(model)

                else -> {
                    val n = Note(NoteType.UNKNOWN)
                    n.populateFromModel(model)
                    return n
                }
            }
        }
    }

    fun populateFromModel(model: NoteModel) {
        time = model.time
        positionX = model.positionX
        speed = model.speed
        floorPosition = model.floorPosition
    }

    open fun getClearTime() = time

    open fun isMissed(): Boolean {
        val game = game!!
        val parent = parent!!

        val gt = parent.getConvertedGameTime(game.time)
        return gt > getClearTime() && !cleared
    }

    open fun isNegativeFloor(): Boolean {
        val parent = parent!!
        return parent.getCurrentFloorPosition() > floorPosition + 1
    }

    open fun getAlpha(): Double {
        if (!isMissed()) return if (isNegativeFloor()) 0.0 else 1.0

        val game = game!!
        val parent = parent!!

        val gt = parent.getConvertedGameTime(game.time)
        val clearTime = getClearTime()
        if (gt < clearTime) return 1.0

        var alpha = 0.5
        val a0time = parent.getConvertedGameTime(parent.getRealTimeMillis(clearTime) + 250)
        val progress = ((gt - clearTime) / (a0time - clearTime)).clamp(0.0, 1.0)
        alpha *= 1 - progress
        return alpha
    }

    open fun update() {
        val game = game!!
        val parent = parent!!

        val gt = parent.getConvertedGameTime(game.time)
        val cleared = gt >= getClearTime()
        val crossed = gt >= time

        if (!cleared) this.cleared = false
        if (!crossed) this.crossed = false

        if (cleared && !this.cleared) {
            this.cleared = true
        }

        if (crossed && !this.crossed) {
            this.crossed = true

            if (gt - time < 10 && game.isPlaying) {
                val ctx = game.audioContext
                if (game.config.enableClickSound && ctx.state == AudioContextState.RUNNING) {
                    val node = ctx.createBufferSource()
                    val fx = audioFx
                    node.buffer = fx

                    node.connect(game.fxCompressor)
                    node.addEventListener("ended", {
                        node.disconnect()
                    })

                    node.start(0.0)
                }

                spawnJudge()
            }
        }
    }

    open fun spawnJudge() {
        val game = game!!
        if (!game.config.enableParticles) return

        val parent = parent!!
        val time = if (this is HoldNote) game.time else parent.getRealTimeMillis(time)
        val linePos = parent.getScaledPosition(parent.getLinePosition(time))
        val rad = -parent.getLineRotation(time) / 180 * PI
        val xPos = getXPos()

        val px = cos(rad) * xPos + linePos.x
        val py = sin(rad) * xPos + linePos.y
        val j = JudgeEffect(game, px, py, 4)
        game.animatedObjects.add(j)
    }

    open fun getXPos(): Double {
        val game = game!!
        val parent = parent!!

        val xPos = 0.845 * positionX / 15
        val rp = parent.getRotatedPosition(Vector2(xPos, 0.0))
        val sp = parent.getScaledPosition(rp)
        sp.x -= game.getRenderXPad()
        sp.y -= game.canvas.height
        sp.y *= 1.8 * game.getNoteRatio() / game.ratio

        val fp = parent.getRotatedPosition(sp)
        return fp.x
    }

    open fun getYPos(): Double {
        val parent = parent!!
        return parent.getYPosByFloorPos(floorPosition - parent.getCurrentFloorPosition())
    }

    open fun isOffscreen(): Boolean {
        if (this is HoldNote) {
            val endTime = time + holdTime
            val gt = parent!!.getConvertedGameTime(game!!.time)
            if (gt > time && gt <= endTime) return false
        }

        return abs(floorPosition - parent!!.getCurrentFloorPosition()) > 5
    }

    open fun render() {
        val game = game!!
        val parent = parent!!
        val ctx = game.context
        ctx.globalAlpha = getAlpha()

        if (game.config.renderDebug && !cleared && (!isOffscreen() || game.config.offScreenForceRender)) {
            val yPos = -getYPos() * (if (this is HoldNote) 1.0 else speed) + 40 * game.ratio
            val xPos = getXPos()

            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.fillStyle = "#fff"
            ctx.font = "${28 * game.ratio}px ${Assets.preferredFont}"
            ctx.fillText("bpm=${parent.bpm} t=${time / 32} f=${floorPosition}", xPos, yPos)
        }
    }

    fun renderTouchArea() {

    }

    protected fun drawSimpleNote(texture: Image) {
        val game = game!!
        val ctx = game.context
        val ratio = game.getNoteRatio()
        val yPos = getYPos() * speed
        val xPos = getXPos()

        val w = noteWidth * ratio
        val h = texture.height.toDouble() / texture.width * w

        if (!cleared) {
            ctx.drawImage(texture, -w / 2 + xPos, -h / 2 - yPos, w, h)
        }
    }

    open val audioFx get() = game!!.assets.audio("fx")
}

