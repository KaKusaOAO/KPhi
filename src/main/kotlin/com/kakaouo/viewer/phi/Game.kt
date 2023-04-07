package com.kakaouo.viewer.phi

import Assets
import com.kakaouo.viewer.phi.Utils.saveTransform
import com.kakaouo.viewer.phi.Utils.saved
import com.kakaouo.viewer.phi.Utils.toCoroutine
import com.kakaouo.viewer.phi.chart.Chart
import com.kakaouo.viewer.phi.chart.LineTexture
import com.kakaouo.viewer.phi.model.ChartModel
import com.kakaouo.viewer.phi.polyfill.*
import com.kakaouo.viewer.phi.polyfill.audio.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

open class Game(val canvas: HTMLCanvasElement) {
    val config = GameConfig()

    var isPlaying = false
    var ratio = 1.0

    val assets = Assets(this)
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    val audioElem = document.getElementById("game-audio") as HTMLAudioElement
    var audioSource: AudioBufferSourceNode? = null

    var chart: Chart? = null

    var time = 0.0
    var performance: ChartPerformance = ChartPerformance.NONE

    val audioContext = AudioContext()
    var audioData: AudioBuffer? = null
    val audioAnalyser: AnalyserNode
    val mainGainNode: GainNode
    val fxGainNode: GainNode
    val fxCompressor: DynamicsCompressorNode
    val audioAnalyseBuffer: Uint8Array

    val touches = mutableListOf<TouchInput>()
    val animatedObjects = mutableListOf<AnimatedObject>()

    var deltaUpdateTime = 0.0
    var lastUpdateTime = 0.0
    var deltaRenderTime = 0.0
    var lastRenderTime = 0.0

    var songName = ""
    var diffName = "IN"
    var diffLevel = 12
    var background: Image? = null

    var blurOffscreenCanvas: HTMLCanvasElement? = null
    var blurOffscreenContext: CanvasRenderingContext2D? = null

    init {
        setResolutionScale(window.devicePixelRatio)

        canvas.addEventListener("touchstart", {
            it.preventDefault()
            val touches = (it as? TouchEvent)?.changedTouches?.asList() ?: listOf()
            for (t in touches) {
                handleTouchStart(t.identifier.toString(), t.pageX, t.pageY)
            }
        })

        canvas.addEventListener("touchend", {
            it.preventDefault()
            val touches = (it as? TouchEvent)?.changedTouches?.asList() ?: listOf()
            for (t in touches) {
                handleTouchEnd(t.identifier.toString())
            }
        })

        canvas.addEventListener("touchcancel", {
            it.preventDefault()
            val touches = (it as? TouchEvent)?.changedTouches?.asList() ?: listOf()
            for (t in touches) {
                handleTouchCancel(t.identifier.toString())
            }
        })

        canvas.addEventListener("mousedown", {
            it.preventDefault()
            val e = it as MouseEvent
            handleTouchStart("mouse", e.pageX.toInt(), e.pageY.toInt())
        })

        canvas.addEventListener("mouseup", {
            it.preventDefault()
            handleTouchEnd("mouse")
        })

        canvas.addEventListener("mouseout", {
            it.preventDefault()

            val t = touches.find { n -> n.id == "mouse" }
            if (t != null) {
                handleTouchEnd("mouse")
            }
        })

        mainGainNode = audioContext.createGain()
        mainGainNode.gain.value = 1
        mainGainNode.connect(audioContext.destination)

        audioAnalyser = audioContext.createAnalyser()
        audioAnalyser.connect(mainGainNode)

        val fb = round(audioAnalyser.frequencyBinCount / 1024.0 * 64).toInt()
        audioAnalyseBuffer = Uint8Array(fb)

        fxGainNode = audioContext.createGain()
        fxGainNode.gain.value = 0.35
        fxGainNode.connect(audioContext.destination)

        fxCompressor = audioContext.createDynamicsCompressor()
        fxCompressor.connect(fxGainNode)

        setupAudio()
        setupImageAssets()
        setupAudioAssets()

        update()
        fixedUpdate()
        render()
    }

    fun useChart(chart: Chart?) {
        this.chart = chart
        chart?.bindContext(this)
    }

    private fun setupAudio() {
        val ctx = audioContext
        val source = ctx.createMediaElementSource(audioElem)
        val gain = ctx.createGain()
        gain.gain.value = 0
        source.connect(gain).connect(ctx.destination)

        audioElem.volume = 0.3
        audioElem.addEventListener("play", {
            isPlaying = true

            val s = audioSource!!
            s.playbackRate.value = audioElem.playbackRate
            s.loop = audioElem.loop
            s.start(0.0, audioElem.currentTime)
        })

        audioElem.addEventListener("pause", {
            isPlaying = false

            val s = audioSource!!
            s.stop()
            s.disconnect()

            val src = AudioBufferSourceNode(ctx)
            src.buffer = audioData
            src.connect(audioAnalyser)
            audioSource = src
        })
    }

    private fun setupImageAssets() {
        assets.loadImageAssetAsync("tap", "assets/Tap2.png")
        assets.loadImageAssetAsync("flick", "assets/Flick2.png")
        assets.loadImageAssetAsync("catch", "assets/Drag (1).png")
        assets.loadImageAssetAsync("tapHL", "assets/Tap2HL.png")
        assets.loadImageAssetAsync("flickHL", "assets/Flick2HL.png")
        assets.loadImageAssetAsync("catchHL", "assets/DragHL.png")
        assets.loadImageAssetAsync("hold", "assets/Hold.png")
        assets.loadImageAssetAsync("holdHead", "assets/Hold_Head.png")
        assets.loadImageAssetAsync("holdEnd", "assets/Hold_End.png")
        assets.loadImageAssetAsync("holdHL", "assets/Hold2HL_1.png")
        assets.loadImageAssetAsync("holdHLHead", "assets/Hold2HL_0.png")
    }

    private fun setupAudioAssets() {
        assets.loadAudioAssetAsync("fx", "assets/fx1.wav")
        assets.loadAudioAssetAsync("catchFx", "assets/click_01.wav")
        assets.loadAudioAssetAsync("flickFx", "assets/drag_01.wav")
    }

    fun setResolutionScale(sc: Double) {
        ratio = sc

        val screen = window.screen
        canvas.width = max(sc * screen.width, sc * screen.height).toInt()
        canvas.height = min(sc * screen.width, sc * screen.height).toInt()
    }

    private fun convertTouchXY(x: Int, y: Int): Vector2 {
        var offsetLeft = canvas.offsetLeft.toDouble()
        var offsetTop = canvas.offsetTop.toDouble()

        if (canvas.classList.contains("fullscreen")) {
            offsetLeft = document.body!!.scrollLeft
            offsetTop = document.body!!.scrollTop
        }

        return Vector2(
            (x - offsetLeft) / canvas.offsetWidth * canvas.width,
            (y - offsetTop) / canvas.offsetHeight * canvas.height
        )
    }

    private fun handleTouchStart(id: String, x: Int, y: Int) {
        val nt = TouchInput(this)
        val cv = convertTouchXY(x, y)
        nt.xPos = cv.x
        nt.yPos = cv.y
        nt.id = id
        touches.add(nt)
    }

    private fun handleTouchEnd(id: String) {
        val nt = touches.indexOfFirst { it.id == id }
        if (nt == -1) {
            console.warn("touchEnd: TouchInput id $id not found!")
            return
        }

        touches.removeAt(nt)
    }

    private fun handleTouchCancel(id: String) {
        val nt = touches.indexOfFirst { it.id == id }
        if (nt == -1) {
            console.warn("touchCancel: TouchInput id $id not found!")
            return
        }

        touches.removeAt(nt)
    }

    fun getRenderXPad(): Double {
        val maxRatio = config.maxRatio
        if (canvas.width.toDouble() / canvas.height > maxRatio) {
            return canvas.width.toDouble() - (canvas.height * maxRatio) / 2
        }

        return 0.0
    }

    suspend fun loadChartWithAudio(chartPath: String, audioPath: String,
                                   lineTextures: Array<LineTexture>? = null,
                                   performance: ChartPerformance = ChartPerformance.NONE
    ) {
        fetch(chartPath, RequestInit(cache = RequestCache.NO_CACHE)) {
            json<ChartModel> {
                val chart = Chart.deserialize(it)
                useChart(chart)

                handleLineTextures(lineTextures)
                this@Game.performance = performance

                loadAudio(audioPath)
            }
        }
    }

    suspend fun loadAudio(audioPath: String) {
        isPlaying = false
        audioElem.pause()
        audioElem.src = audioPath

        fetch(audioPath) {
            arrayBuffer {
                val audio = audioContext.decodeAudioData(it).toCoroutine()
                audioData = audio

                val source = audioContext.createBufferSource()
                source.buffer = audio
                replaceAudio(source)
            }
        }
    }

    fun replaceAudio(source: AudioBufferSourceNode) {
        audioSource?.disconnect()

        source.connect(audioAnalyser)
        audioSource = source
    }

    fun setPlaybackRate(rate: Double) {
        audioElem.playbackRate = rate
        audioSource!!.playbackRate.value = rate
    }

    private fun updateTime(isRender: Boolean) {
        val dt = if (isRender) deltaRenderTime else deltaUpdateTime
        val offset = (chart?.offset ?: 0.0) * 1000 + config.audioOffset
        val smooth = (dt / config.smooth).clamp(0.0, 1.0)

        if (!isPlaying) {
            if (!isRender) return
            time = smooth.lerp(time, audioElem.currentTime * 1000 + offset)
        } else {
            if (isRender) return

            time += dt * audioElem.playbackRate
            val actualTime = audioElem.currentTime * 1000 + offset
            if (abs(time - actualTime) > 16 * audioElem.playbackRate) time = actualTime
        }
    }

    fun update() {
        window.setTimeout({ update() }, 0)
        mainGainNode.gain.value = audioElem.volume

        val p = window.performance.now()
        deltaUpdateTime = p - lastUpdateTime
        lastUpdateTime = p
        updateTime(false)

        audioAnalyser.getByteFrequencyData(audioAnalyseBuffer)
        animatedObjects.removeAll {
            it.update()
            it.notNeeded
        }

        chart?.judgeLineList?.forEach { it.update() }
    }

    fun fixedUpdate() {
        window.setTimeout({ fixedUpdate() }, 5)
        animatedObjects.forEach { it.fixedUpdate() }
    }

    fun render() {
        val useAnimationFrame = config.useAnimationFrame
        val maxFps = config.maxFps
        val refScreenWidth = config.refScreenWidth
        val refScreenHeight = config.refScreenHeight

        if (useAnimationFrame) {
            window.requestAnimationFrame { render() }
        } else {
            window.setTimeout({ render() }, 0)
        }

        val p = window.performance.now()
        deltaRenderTime = p - lastRenderTime

        if (deltaRenderTime < 1000 / maxFps) {
            return
        }

        lastRenderTime = p
        updateTime(true)

        val refAspect = refScreenWidth / refScreenHeight
        val width = canvas.width - getRenderXPad()
        val height = canvas.height.toDouble()
        val aspect = width / height
        ratio = if (aspect > refAspect) {
            height / refScreenHeight
        } else {
            width / refScreenWidth
        }

        renderReset()
        renderBack()
        renderJudgeLines()
        animatedObjects.forEach { it.render() }
        renderUI()

        touches.forEach {
            if (it.state == 0) it.state = 1
        }

        if (window.innerWidth < window.innerHeight) {
            canvas.classList.remove("fullscreen")
        }
    }

    protected open fun renderReset() {
        val ctx = context
        ctx.restore()
        ctx.textAlign = CanvasTextAlign.LEFT
        ctx.fillStyle = "#000"
    }

    protected open fun renderBack() {
        val ctx = context
        ctx.fillRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())

        val bg = background
        if (bg == null) return

        try {
            val pad = getRenderXPad()
            val iw = bg.width.toDouble()
            val ih = bg.height.toDouble()

            var osc = blurOffscreenCanvas
            if (osc == null) {
                osc = document.createElement("canvas") as HTMLCanvasElement
                blurOffscreenCanvas = osc
            }

            osc.width = ctx.canvas.width
            osc.height = ctx.canvas.height

            var oCtx = blurOffscreenContext
            if (oCtx == null) {
                oCtx = osc.getContext("2d") as CanvasRenderingContext2D
                blurOffscreenContext = oCtx
            }

            val ox: Double
            val oy: Double
            val ow: Double
            val oh: Double

            if (iw / ih > ctx.canvas.width.toDouble() / ctx.canvas.height) {
                val xOffset = (ctx.canvas.width - pad * 2) - ctx.canvas.height / ih * iw
                ox = pad + xOffset / 2 - ctx.canvas.width / 2
                oy = -ctx.canvas.height / 2.0
                ow = iw / ih * oCtx.canvas.height
                oh = oCtx.canvas.height.toDouble()
            } else {
                val xOffset = -pad * 2
                val yOffset = ctx.canvas.height.toDouble() - ctx.canvas.width / iw * ih
                ox = pad + xOffset / 2 - ctx.canvas.width / 2
                oy = -ctx.canvas.height / 2.0 + yOffset / 2
                ow = oCtx.canvas.width.toDouble()
                oh = oCtx.canvas.width.toDouble() * ih / iw
            }

            ctx.globalAlpha = 0.5
            ctx.saveTransform {
                ctx.translate(ctx.canvas.width / 2.0, ctx.canvas.height / 2.0)
                ctx.drawImage(bg, ox, oy, ow, oh)
            }

            ctx.globalAlpha = 1.0
            ctx.saved {
                ctx.beginPath()
                ctx.rect(pad, 0.0, ctx.canvas.width - pad * 2, ctx.canvas.height.toDouble())
                ctx.clip()

                oCtx.saveTransform {
                    val backgroundBlur = config.backgroundBlur
                    if (backgroundBlur > 0) {
                        oCtx.filter = "blur(${backgroundBlur * ratio}px)"
                    } else {
                        oCtx.filter = "none"
                    }

                    oCtx.translate(ctx.canvas.width / 2.0, ctx.canvas.height / 2.0)
                    oCtx.scale(1.0, 1.0)
                    oCtx.drawImage(bg, ox, oy, ow, oh)
                }

                ctx.drawImage(osc, 0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())

                // Dim
                val backgroundDim = config.backgroundDim
                ctx.globalAlpha = backgroundDim
                ctx.fillRect(pad, 0.0, ctx.canvas.width - pad * 2, ctx.canvas.height.toDouble())
            }

            ctx.globalAlpha = 1.0
        } catch (err: Throwable) {
            console.error(err)

            ctx.fillStyle = "#000"
            ctx.globalAlpha = 1.0
            ctx.fillRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())

            ctx.fillStyle = "#fff"
            ctx.globalAlpha - 0.25
            ctx.textBaseline = CanvasTextBaseline.MIDDLE
            ctx.textAlign = CanvasTextAlign.CENTER

            val ratio = ratio * 1.25
            ctx.font = "${36 * ratio}px ${Assets.preferredFont}"
            ctx.fillText("Invalid background!", ctx.canvas.width / 2.0, ctx.canvas.height / 2.0, ctx.canvas.width * 0.8)

            ctx.globalAlpha = 1.0
        }
    }

    protected open fun renderJudgeLines() {
        val ctx = context
        ctx.saved {
            ctx.beginPath()
            ctx.rect(getRenderXPad(), 0.0, ctx.canvas.width - getRenderXPad() * 2, ctx.canvas.height.toDouble())
            ctx.clip()

            val chart = chart ?: return@saved
            chart.judgeLineList.forEach { it.renderNotes() }
            chart.judgeLineList.forEach { it.renderLine() }
        }

        ctx.fillStyle = "#fff"
    }

    protected open fun renderUI() {

    }

    fun handleLineTextures(lineTextures: Array<LineTexture>?) {
        if (lineTextures == null) return
        lineTextures.forEach {
            val line = chart!!.judgeLineList[it.index]
            val image = Image()
            image.src = it.image
            line.texture = image
            line.texturePos = Vector2(it.pos[0], it.pos[1]) // ?: Vector2(1.0, 1.0)
        }
    }

    fun getNoteRatio(): Double {
        val w = canvas.width - getRenderXPad() * 2
        val h = canvas.height.toDouble()
        val aspect = w / h
        val refAspect = 1920 / 1080.0

        val mult = if (aspect >= refAspect) 1.0 else lerp(1.0, aspect / refAspect, 0.8)
        return ratio * mult
    }
}

