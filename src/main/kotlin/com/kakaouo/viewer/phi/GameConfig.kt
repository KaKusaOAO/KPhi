@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package com.kakaouo.viewer.phi

class GameConfig {
    var enableParticles = true
    var enableClickSound = true
    var renderDebug = false
    var refScreenWidth = 1440.0
    var refScreenHeight = 1080.0
    var maxRatio = 16.0 / 9
    var backgroundBlur = 20.0
    var backgroundDim = 0.66
    var offScreenForceRender = false
    var maxFps = 600
    var smooth = 100.0
    var audioOffset = 0f
    var useAnimationFrame = true
}