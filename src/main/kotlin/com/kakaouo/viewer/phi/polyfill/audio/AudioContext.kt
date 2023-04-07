package com.kakaouo.viewer.phi.polyfill.audio

import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLMediaElement
import kotlin.js.Promise

open external class AudioContext {
    open val state: AudioContextState
    open val audioWorklet: AudioWorklet
    open val currentTime: Number
    open val destination: AudioDestinationNode
    open val listener: AudioListener

    open fun decodeAudioData(audioData: ArrayBuffer,
                             successCallback: DecodeSuccessCallback? = definedExternally,
                             errorCallback: DecodeErrorCallback? = definedExternally): Promise<AudioBuffer>
    open fun createDynamicsCompressor(): DynamicsCompressorNode
    open fun createBufferSource(): AudioBufferSourceNode
    open fun createGain(): GainNode
    open fun createAnalyser(): AnalyserNode
    open fun createMediaElementSource(mediaElement: HTMLMediaElement): MediaElementAudioSourceNode
}