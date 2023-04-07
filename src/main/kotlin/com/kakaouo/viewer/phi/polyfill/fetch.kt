package com.kakaouo.viewer.phi.polyfill

import com.kakaouo.viewer.phi.Utils.toCoroutine
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

class FetchDSL(private val response: Response) {
    suspend fun json(block: suspend (Any?) -> Unit) {
        val result = response.json().toCoroutine()
        block(result)
    }

    suspend fun <T> json(block: suspend (T) -> Unit) {
        val result = response.text().toCoroutine()
        block(JSON.parse(result))
    }

    suspend fun text(block: suspend (String) -> Unit) {
        val result = response.text().toCoroutine()
        block(result)
    }

    suspend fun arrayBuffer(block: suspend (ArrayBuffer) -> Unit) {
        val result = response.arrayBuffer().toCoroutine()
        block(result)
    }
}

suspend fun fetch(url: String, block: suspend FetchDSL.() -> Unit) {
    val response = window.fetch(url).toCoroutine()
    val dsl = FetchDSL(response)
    dsl.block()
}

suspend fun fetch(url: String, init: RequestInit, block: suspend FetchDSL.() -> Unit) {
    val response = window.fetch(url, init).toCoroutine()
    val dsl = FetchDSL(response)
    dsl.block()
}