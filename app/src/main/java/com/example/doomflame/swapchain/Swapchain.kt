package com.example.doomflame.swapchain

interface Swapchain<T> {
    fun acquire(): T
    fun release(value: T)

    fun acquireDirty(): T
    fun releaseUpdated(value: T)
}

inline fun <T, R> Swapchain<T>.use(action: (T) -> R): R {
    val v = acquire()
    return action(v).also { release(v) }
}

inline fun <T, R> Swapchain<T>.update(action: (T) -> R): R {
    val v = acquireDirty()
    return action(v).also { releaseUpdated(v) }
}
