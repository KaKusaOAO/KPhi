package com.kakaouo.viewer.phi.polyfill.audio

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.events.EventTarget

open external class MediaElementAudioSourceNode : AudioNode {
    open val mediaElement: HTMLMediaElement
}

open external class DynamicsCompressorNode : AudioNode {
    val attack: AudioParam
    val knee: AudioParam
    val ratio: AudioParam
    val reduction: AudioParam
    val release: AudioParam
    val threshold: AudioParam
}

open external class GainNode : AudioNode {
    val gain: AudioParam
}

typealias DecodeSuccessCallback = (AudioBuffer) -> Unit
typealias DecodeErrorCallback = (Throwable) -> Unit

open external class AudioNode : EventTarget {
    var channelCount: Int

    fun connect(destinationNode: AudioNode,
                output: Number = definedExternally,
                input: Number = definedExternally): AudioNode
    fun connect(destinationParam: AudioParam,
                output: Number = definedExternally)
    fun disconnect(output: Number = definedExternally)
    fun disconnect(destinationNode: AudioNode,
                   output: Number = definedExternally,
                   input: Number = definedExternally)
}

open external class AudioDestinationNode : AudioNode {

}

open external class AudioBufferSourceNode(context: AudioContext) : AudioNode {
    open val playbackRate: AudioParam
    open var buffer: AudioBuffer?
    open var loop: Boolean
    open fun start(`when`: Double = definedExternally,
                   offset: Double = definedExternally,
                   duration: Double = definedExternally)
    open fun stop(`when`: Double? = definedExternally)
}

open external class AnalyserNode : AudioNode {
    open var fftSize: Number
    open val frequencyBinCount: Int
    open var maxDecibels: Float
    open var minDecibels: Float
    open var smoothingTimeConstant: Number

    open fun getByteFrequencyData(array: Uint8Array)
    open fun getByteTimeDomainData(array: Uint8Array)
    open fun getFloatFrequencyData(array: Float32Array)
    open fun getFloatTimeDomainData(array: Float32Array)
}

