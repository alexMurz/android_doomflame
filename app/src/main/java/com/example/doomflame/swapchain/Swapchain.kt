package com.example.doomflame.swapchain

interface Swapchain<T> {
    fun consume(action: Consumer<T>)
    fun update(action: Updater<T>)

    fun interface Consumer<T> {
        fun consume(value: T)
    }

    fun interface Updater<T> {
        fun update(value: T)
    }
}
