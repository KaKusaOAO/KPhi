package com.kakaouo.viewer.phi.plugins.arcaea

import com.kakaouo.viewer.phi.ChartPerformance
import com.kakaouo.viewer.phi.Game
import com.kakaouo.viewer.phi.polyfill.fetch
import com.kakaouo.viewer.phi.polyfill.group
import com.kakaouo.viewer.phi.polyfill.groupCollapsed
import com.kakaouo.viewer.phi.polyfill.groupEnd
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit

class ArcaeaChart {
    val timingGroups = arrayListOf<ArcaeaTimingGroup>()
    val metadata = mutableMapOf<String, String>()

    enum class DeserializerState {
        METADATA,
        NOTES
    }

    val offset get() = metadata["AudioOffset"]?.toDouble() ?: 0.0
    val primaryGroup get() = timingGroups.find { it.isPrimary }!!

    companion object {
        suspend fun load(game: Game, basePath: String, diff: Int, arcDensity: Double = 1.0, speedMultiplier: Double = 1.0) {
            fetch("$basePath/$diff.aff", RequestInit(cache = RequestCache.NO_CACHE)) {
                text {
                    val arcaea = deserialize(it)
                    val chart = ArcaeaChartConverter.convert(arcaea, arcDensity, speedMultiplier)
                    game.useChart(chart)
                    game.loadAudio("$basePath/base.ogg")
                    game.performance = ChartPerformance.NONE
                }
            }
        }

        suspend fun loadFromDLC(game: Game, name: String, diff: Int, arcDensity: Double = 1.0, speedMultiplier: Double = 1.0) {
            val basePath = "/arcaea/assets/charts/dl"
            fetch("$basePath/${name}_$diff", RequestInit(cache = RequestCache.NO_CACHE)) {
                text {
                    val arcaea = deserialize(it)
                    val chart = ArcaeaChartConverter.convert(arcaea, arcDensity, speedMultiplier)
                    game.useChart(chart)
                    game.loadAudio("$basePath/$name")
                    game.performance = ChartPerformance.NONE
                }
            }
        }

        fun deserialize(raw: String): ArcaeaChart {
            val chart = ArcaeaChart()
            val primaryGroup = ArcaeaTimingGroup(true)
            chart.timingGroups.add(primaryGroup)

            var targetGroup = primaryGroup
            var state = DeserializerState.METADATA

            console.groupCollapsed("Read chart: ", chart);
            try {
                raw.split('\n').forEach {
                    if (state == DeserializerState.METADATA) {
                        state = processMetadataLine(chart, it)
                        return@forEach
                    }

                    if (state != DeserializerState.NOTES) return@forEach

                    val line = it.trim()
                    if (line.startsWith("timinggroup(")) {
                        ArcaeaTimingGroup.readLine(line).apply {
                            chart.timingGroups.add(this)
                            targetGroup = this
                            console.group("TimingGroup:", this)
                        }
                    } else if (line.startsWith("}")) {
                        targetGroup = primaryGroup
                        console.groupEnd()
                    } else if (line.startsWith("timing(")) {
                        targetGroup.events.add(ArcaeaTimingEvent.readLine(line).apply {
                            console.log("Timing:", this)
                        })
                    } else if (line.startsWith("(")) {
                        targetGroup.events.add(ArcaeaTapEvent.readLine(line).apply {
                            console.log("Tap:", this)
                        })
                    } else if (line.startsWith("arc(")) {
                        targetGroup.events.add(ArcaeaArcEvent.readLine(line).apply {
                            if (this.arcTaps.isNotEmpty()) {
                                console.group("Arc:", this)
                                arcTaps.forEach { at ->
                                    console.log("ArcTap:", at)
                                }
                                console.groupEnd()
                            } else {
                                console.log("Arc:", this)
                            }
                        })
                    } else if (line.startsWith("hold(")) {
                        targetGroup.events.add(ArcaeaHoldEvent.readLine(line).apply {
                            console.log("Hold:", this)
                        })
                    } else if (line.startsWith("camera(")) {
                        // Don't handle camera events
                    } else if (line.startsWith("scenecontrol(")) {
                        targetGroup.events.add(ArcaeaSceneControlEvent.readLine(line).apply {
                            console.log("SceneControl:", this)
                        })
                    } else {
                        console.warn("Unhandled line:", line)
                    }
                }
            } finally {
                console.groupEnd()
            }
            return chart
        }

        private fun processMetadataLine(chart: ArcaeaChart, line: String): DeserializerState {
            if (line.trim() == "-") {
                return DeserializerState.NOTES
            }

            val split = line.split(':', limit = 2)
            chart.metadata[split[0]] = split[1]
            return DeserializerState.METADATA
        }
    }
}

