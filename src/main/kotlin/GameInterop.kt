@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

import com.kakaouo.viewer.phi.*
import com.kakaouo.viewer.phi.chart.LineTexture
import com.kakaouo.viewer.phi.plugins.arcaea.ArcaeaChart
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import kotlin.js.Promise

@Suppress("NON_EXPORTABLE_TYPE")
object GameInterop {
    fun createGame(canvas: HTMLCanvasElement): Game {
        return Phigros(canvas)
    }


    class ChartLoadOptions(val chartPath: String,
                           val audioPath: String,
                           val lineTextures: Array<LineTexture>? = null,
                           val performance: ChartPerformance = ChartPerformance.NONE)

    fun loadChartWithAudio(game: Game, options: ChartLoadOptions): Promise<Unit> {
        return Utils.promisify {
            game.loadChartWithAudio(options.chartPath, options.audioPath, options.lineTextures, options.performance)
        }
    }

    class ChartInfoOptions(val songName: String? = null,
                           val diffName: String? = null,
                           val diffLevel: Int? = null,
                           val background: Image? = null)

    fun updateChartInfo(game: Game, options: ChartInfoOptions) {
        if (options.songName != null) game.songName = options.songName
        if (options.diffName != null) game.diffName = options.diffName
        if (options.diffLevel != null) game.diffLevel = options.diffLevel
        if (options.background != null) game.background = options.background
    }

    fun setPlaybackRate(game: Game, rate: Double) {
        game.setPlaybackRate(rate)
    }

    fun getConfig(game: Game): GameConfig {
        return game.config
    }

    fun setResolutionScale(game: Game, scale: Double) {
        game.setResolutionScale(scale)
    }

    fun addJudgeEffect(game: Game, x: Double, y: Double, amount: Int) {
        game.animatedObjects.add(JudgeEffect(game, x, y, amount))
    }

    fun loadArcaeaAsync(game: Game, basePath: String, diff: Int,
                        arcDensity: Double = 1.0, speedMultiplier: Double = 1.0): Promise<Unit> {
        return Utils.promisify {
            ArcaeaChart.load(game, basePath, diff, arcDensity, speedMultiplier)
        }
    }

    fun loadArcaeaFromDLCAsync(game: Game, name: String, diff: Int,
                               arcDensity: Double = 1.0, speedMultiplier: Double = 1.0): Promise<Unit> {
        return Utils.promisify {
            ArcaeaChart.loadFromDLC(game, name, diff, arcDensity, speedMultiplier)
        }
    }
}