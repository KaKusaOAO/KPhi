import com.kakaouo.viewer.phi.model.NoteModel

class TapNote : Note(NoteType.TAP) {
    companion object {
        fun deserialize(model: NoteModel): TapNote {
            val result = TapNote()
            result.populateFromModel(model)
            return result
        }
    }

    override fun render() {
        super.render()
        val game = game!!
        if (isOffscreen() && !game.config.offScreenForceRender) return

        val texture = game.assets.image(if (hasSibling) "tapHL" else "tap")!!
        drawSimpleNote(texture)
    }
}