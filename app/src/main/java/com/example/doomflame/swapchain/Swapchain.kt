package com.example.doomflame.swapchain

interface Swapchain<T> {
    fun use(action: (T) -> Unit)
    fun refresh(action: (T) -> Unit)
}
