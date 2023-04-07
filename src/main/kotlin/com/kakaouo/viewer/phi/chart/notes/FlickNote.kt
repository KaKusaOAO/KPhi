import com.kakaouo.viewer.phi.model.NoteModel
import com.kakaouo.viewer.phi.polyfill.audio.AudioBuffer

class FlickNote : Note(NoteType.FLICK) {
    companion object {
        fun deserialize(model: NoteModel): FlickNote {
            val result = FlickNote()
            result.populateFromModel(model)
            return result
        }
    }

    override val audioFx: AudioBuffer?
        get() = game!!.assets.audio("flickFx")

    override fun render() {
        super.render()
        val game = game!!
        if (isOffscreen() && !game.config.offScreenForceRender) return

        val texture = game.assets.image(if (hasSibling) "flickHL" else "flick")!!
        drawSimpleNote(texture)
    }
}