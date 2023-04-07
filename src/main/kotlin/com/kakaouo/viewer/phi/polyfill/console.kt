package com.kakaouo.viewer.phi.polyfill

import kotlin.js.Console

inline fun Console.group(vararg o: Any) {
    asDynamic().group.apply(this, o)
}

inline fun Console.groupCollapsed(vararg o: Any) {
    asDynamic().groupCollapsed.apply(this, o)
}

inline fun Console.groupEnd() {
    asDynamic().groupEnd()
}