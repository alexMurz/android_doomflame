package com.example.doomflame.swapchain

import java.util.concurrent.atomic.AtomicBoolean

class DoubleBufferingSwapchain<T>(
    factory: ItemFactory<T>,
) : Swapchain<T> {
    private val first = factory.create()
    private val second = factory.create()
    private val state = AtomicBoolean(false)

    private val refreshLock = Any()
    private val useLock = Any()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun get(
        stateValue: Boolean = state.get(),
        reverse: Boolean = false,
    ): T = when (stateValue xor reverse) {
        true -> second
        else -> first
    }


    override fun consume(action: Swapchain.Consumer<T>): Unit = synchronized(useLock) {
        action.consume(get())
    }

    override fun update(action: Swapchain.Updater<T>): Unit = synchronized(refreshLock) {
        val stateValue = state.get()
        val item = get(
            stateValue = stateValue,
            reverse = true,
        )

        action.update(item)
        synchronized(useLock) {
            if (!state.compareAndSet(stateValue, !stateValue)) {
                throw IllegalStateException(
                    "Swapchain poisoning! inconsistent state while updating"
                )
            }
        }
    }

    fun interface ItemFactory<T> {
        fun create(): T
    }

}