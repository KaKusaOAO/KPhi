import com.kakaouo.viewer.phi.ChartPerformance
import com.kakaouo.viewer.phi.Phigros
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image

suspend fun main() {
    val canvas = document.getElementById("main") as HTMLCanvasElement

    val game = Phigros(canvas)
    console.log(game)
    window.asDynamic().game = game

    game.run {
        songName = "今年も「雪降り、メリクリ」目指して頑張ります！！"
        diffName = "SP"
        diffLevel = -1

        val bg = Image()
        bg.src = "./assets/charts/apf2023/bg.png"
        background = bg
    }
    game.loadChartWithAudio(
        "./assets/charts/apf2023/2.json",
        "./assets/charts/apf2023/base.ogg",
        performance = ChartPerformance.APF_2023)
}