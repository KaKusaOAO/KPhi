import com.kakaouo.viewer.phi.Utils.promisify
import com.kakaouo.viewer.phi.Utils.toCoroutine
import com.kakaouo.viewer.phi.Game
import kotlinx.coroutines.yield
import org.w3c.dom.Image
import com.kakaouo.viewer.phi.polyfill.audio.AudioBuffer
import com.kakaouo.viewer.phi.polyfill.fetch
import kotlin.js.Promise

class Assets(val game: Game) {
    companion object {
        private val imageAssets: MutableMap<String, Image> = mutableMapOf()
        private val audioAssets: MutableMap<String, AudioBuffer> = mutableMapOf()
        val preferredFont = """Saira, Exo, "Noto Sans CJK TC", sans-serif"""
    }

    suspend fun loadImageAsset(name: String, source: String): Image {
        if (imageAssets.containsKey(name)) {
            return imageAssets[name]!!
        }

        val img = Image()
        var loaded = false
        img.onload = fun(_) {
            imageAssets[name] = img
            loaded = true
        }
        img.src = source

        while (!loaded) {
            yield()
        }

        return img
    }

    fun loadImageAssetAsync(name: String, source: String): Promise<Image> {
        return promisify {
            loadImageAsset(name, source)
        }
    }

    suspend fun loadAudioAsset(name: String, source: String): AudioBuffer? {
        if (audioAssets.containsKey(name)) return audioAssets[name]
        val ctx = game.audioContext // ?: return null

        var buffer: AudioBuffer? = null
        fetch(source) {
            arrayBuffer {
                buffer = ctx.decodeAudioData(it).toCoroutine()
            }
        }

        if (buffer != null) {
            audioAssets[name] = buffer!!
        }
        return buffer
    }

    fun loadAudioAssetAsync(name: String, source: String): Promise<AudioBuffer?> {
        return promisify {
            loadAudioAsset(name, source)
        }
    }

    fun image(name: String): Image? = imageAssets[name]
    fun audio(name: String): AudioBuffer? = audioAssets[name]
}