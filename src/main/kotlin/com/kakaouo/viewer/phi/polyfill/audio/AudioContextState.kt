package com.kakaouo.viewer.phi.polyfill.audio

interface AudioContextState {
    companion object {
        val CLOSED get() = "closed".asDynamic().unsafeCast<AudioContextState>()
        val RUNNING get() = "running".asDynamic().unsafeCast<AudioContextState>()
        val SUSPENDED get() = "suspended".asDynamic().unsafeCast<AudioContextState>()
    }
}