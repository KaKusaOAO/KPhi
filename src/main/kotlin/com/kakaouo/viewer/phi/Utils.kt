package com.kakaouo.viewer.phi

import com.kakaouo.viewer.phi.chart.events.ChartEvent
import kotlinx.coroutines.*
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Promise
import kotlin.math.floor

object Utils {
    suspend fun <T> Promise<T>.toCoroutine(): T {
        var completed = false
        var faulted = false
        var result: T? = null
        var err: Throwable? = null

        this.then {
            completed = true
            result = it
        }.catch {
            completed = true
            faulted = true
            err = it
        }

        while (!completed) {
            delay(16)
        }

        if (faulted) {
            throw err!!
        }

        return result!!
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <T> promisify(block: suspend () -> T): Promise<T> {
        return Promise { resolve, reject ->
            @Suppress("DeferredResultUnused")
            GlobalScope.async {
                try {
                    resolve(block())
                } catch (err: Throwable) {
                    reject(err)
                }
            }
        }
    }

    fun <T: ChartEvent> List<T>.findEvent(time: Double, start: Int = 0, end: Int = this.size - 1): T? {
        // Base Condition
        if (start > end) return null

        val mid = floor((start + end) / 2.0).toInt()
        val e = this[mid]
        if (time > e.startTime && time <= e.endTime) return e

        return if (e.startTime >= time) {
            findEvent(time, start, mid - 1)
        } else {
            findEvent(time, mid + 1, end)
        }
    }

    fun <T: ChartEvent> List<T>.findEventOrFirst(time: Double): T? {
        return findEvent(time) ?: if (isNotEmpty()) this[0] else return null;
    }

    fun CanvasRenderingContext2D.saved(block: () -> Unit) {
        save()
        try {
            block()
        } finally {
            restore()
        }
    }

    fun CanvasRenderingContext2D.saveTransform(block: () -> Unit) {
        val t = getTransform()
        try {
            block()
        } finally {
            setTransform(t)
        }
    }

    fun Uint8Array.forEach(block: (Byte) -> Unit) {
        for (i in 0 until length) {
            block(this[i])
        }
    }

    fun Uint8Array.average(): Double {
        var f = 0.0
        forEach { f += it }
        return f / length
    }
}