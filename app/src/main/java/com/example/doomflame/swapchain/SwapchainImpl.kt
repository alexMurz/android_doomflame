package com.example.doomflame.swapchain

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * Typical 3 buffer unsafe swapchain
 * Supports chainLength from 3 at above
 *
 *
 * Will throw if cannot update or read
 * Technically can create collision between readers and writer
 * is cases of multiple, slow consumers
 * but whatever, not our use case
 */
class SwapchainImpl<T: Any>(
    chainLength: Int = 3,
    factory: ItemFactory<T>,
) : Swapchain<T> {
    init {
        require(chainLength >= 3) {
            "SwapchainImpl only supports chains of length of 3 or more"
        }
    }

    private val producerCache = ArrayDeque<ChainNode<T>>(chainLength)

    /**
     * Ordered chain
     * From newest to oldest
     */
    private val chain = ConcurrentLinkedDeque<ChainNode<T>>().apply {
        repeat(chainLength) {
            add(ChainNode(factory.create()))
        }
    }

    override fun consume(action: Swapchain.Consumer<T>) {
        val node: ChainNode<T> = chain.first
        node.readers.incrementAndGet()
        action.consume(node.item)
        node.readers.decrementAndGet()
    }

    override fun update(action: Swapchain.Updater<T>): Unit = synchronized(this) {
        var node = chain.removeLast()
        while (node.readers.get() > 0) {
            producerCache.addLast(node)
            node = chain.removeLast()
        }

        while (producerCache.size > 0) {
            chain.addLast(producerCache.removeFirst())
        }

        action.update(node.item)
        chain.addFirst(node)

        println(chain.joinToString())
    }

    private class ChainNode<out T>(
        val item: T,
        val readers: AtomicInteger = AtomicInteger(0),
    ) {
        override fun toString() = "Node(${readers.get()})"
    }

    fun interface ItemFactory<T> {
        fun create(): T
    }
}