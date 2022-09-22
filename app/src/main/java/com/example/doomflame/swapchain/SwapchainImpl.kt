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
class SwapchainImpl<T : Any>(
    chainLength: Int = 3,
    factory: ItemFactory<T>,
) : Swapchain<T> {
    init {
        require(chainLength >= 3) {
            "SwapchainImpl only supports chains of length of 3 or more"
        }
    }

    internal val producerCache = ArrayDeque<ChainNode<T>>(chainLength)

    /**
     * Ordered chain
     * From newest to oldest
     */
    internal val chain = ConcurrentLinkedDeque<ChainNode<T>>().apply {
        repeat(chainLength) {
            add(ChainNode(factory.create()))
        }
    }

    internal val nodeMap = chain.associateBy { it.item }

    internal fun nodeForValue(value: T) =
        nodeMap[value] ?: throw IllegalArgumentException("Value is not a member of this Swapchain")

    override fun acquire(): T {
        val node = chain.first
        node.readers.incrementAndGet()
        return node.item
    }

    override fun release(value: T) {
        val node = nodeForValue(value)
        node.readers.decrementAndGet()
    }

    override fun acquireDirty(): T {
        var node = chain.removeLast()
        while (node.readers.get() > 0) {
            producerCache.addLast(node)
            node = chain.removeLast()
        }

        while (producerCache.isNotEmpty()) {
            chain.addLast(producerCache.removeFirst())
        }

        return node.item
    }

    override fun releaseUpdated(value: T) {
        val node = nodeForValue(value)
        chain.addFirst(node)
    }

    internal class ChainNode<out T>(
        val item: T,
        val readers: AtomicInteger = AtomicInteger(0),
    ) {
        override fun toString() = "Node(readers:${readers.get()}, value:$item)"
    }

    fun interface ItemFactory<T> {
        fun create(): T
    }
}