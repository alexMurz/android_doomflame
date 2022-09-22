package com.example.doomflame.swapchain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.junit.Assert
import org.junit.Test

internal class SwapchainImplTest {

    data class Val(
        val id: Int,
        var i: Int
    ) {
        override fun equals(other: Any?): Boolean = when (other) {
            is Val -> id == other.id
            else -> false
        }

        override fun hashCode(): Int = id.hashCode()
    }

    // Start updating on emit true
    // Stop updating on emit false
    private fun CoroutineScope.createUpdateTrigger(swapchain: Swapchain<Val>): MutableStateFlow<Boolean> {
        val updateTrigger = MutableStateFlow(false)

        updateTrigger
            .filter {
                it
            }
            .onEach {
                swapchain.update {
                    it.i += 1
                    updateTrigger.filter { !it }.first()
                }
            }
            .launchIn(this)

        return updateTrigger
    }

    // Start reading on emit true
    // Stop reading on emit false
    private fun CoroutineScope.createReadTrigger(swapchain: Swapchain<Val>): MutableStateFlow<Boolean> {
        val updateTrigger = MutableStateFlow(false)

        updateTrigger
            .filter { it }
            .onEach {
                swapchain.use {
                    it.i += 1
                    updateTrigger.filter { !it }.first()
                }
            }
            .launchIn(this)

        return updateTrigger
    }

    // map of expected id to value
    private fun SwapchainImpl<Val>.assertReadableState(vararg stateList: Pair<Int, Int>) {
        val state = ArrayDeque(stateList.toList())
        val initialReadable = ArrayDeque(chain)
        val readable = ArrayDeque(initialReadable)
        Assert.assertEquals(initialReadable.joinToString(), readable.size, state.size)
        while (readable.isNotEmpty()) {
            val got = readable.removeFirst()
            val expected = state.removeFirst()
            Assert.assertEquals(initialReadable.joinToString(), expected.first, got.item.id)
            Assert.assertEquals(initialReadable.joinToString(), expected.second, got.readers.get())
        }
    }

    @Test
    fun `should update`() {
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val chain = run {
            var idx = 0
            SwapchainImpl(3) { Val(idx++, 0) }
        }

        val update = scope.createUpdateTrigger(chain)

        chain.assertReadableState(
            0 to 0,
            1 to 0,
            2 to 0,
        )

        update.value = true
        chain.assertReadableState(
            0 to 0,
            1 to 0,
        )

        update.value = false
        chain.assertReadableState(
            2 to 0,
            0 to 0,
            1 to 0,
        )

        update.value = true
        chain.assertReadableState(
            2 to 0,
            0 to 0,
        )

        update.value = false
        chain.assertReadableState(
            1 to 0,
            2 to 0,
            0 to 0,
        )
    }

    @Test
    fun `should read from single node`() {
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val chain = run {
            var idx = 0
            SwapchainImpl(3) { Val(idx++, 0) }
        }

        val read1 = scope.createReadTrigger(chain)
        val read2 = scope.createReadTrigger(chain)

        chain.assertReadableState(
            0 to 0,
            1 to 0,
            2 to 0,
        )

        read1.value = true
        chain.assertReadableState(
            0 to 1,
            1 to 0,
            2 to 0,
        )

        read2.value = true
        chain.assertReadableState(
            0 to 2,
            1 to 0,
            2 to 0,
        )
    }

    @Test
    fun `should read from multiple nodes`() {
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val chain = run {
            var idx = 0
            SwapchainImpl(3) { Val(idx++, 0) }
        }

        val update = scope.createUpdateTrigger(chain)
        val read1 = scope.createReadTrigger(chain)
        val read2 = scope.createReadTrigger(chain)

        chain.assertReadableState(
            0 to 0,
            1 to 0,
            2 to 0,
        )

        read1.value = true
        chain.assertReadableState(
            0 to 1,
            1 to 0,
            2 to 0,
        )

        update.value = true
        chain.assertReadableState(
            0 to 1,
            1 to 0,
        )

        update.value = false
        chain.assertReadableState(
            2 to 0,
            0 to 1,
            1 to 0,
        )

        read2.value = true
        chain.assertReadableState(
            2 to 1,
            0 to 1,
            1 to 0,
        )
    }

    @Test
    fun `should write when last is busy`() {
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val chain = run {
            var idx = 0
            SwapchainImpl(3) { Val(idx++, 0) }
        }

        val update = scope.createUpdateTrigger(chain)
        val read1 = scope.createReadTrigger(chain)

        chain.assertReadableState(
            0 to 0,
            1 to 0,
            2 to 0,
        )

        read1.value = true
        chain.assertReadableState(
            0 to 1,
            1 to 0,
            2 to 0,
        )

        update.value = true
        chain.assertReadableState(
            0 to 1,
            1 to 0,
        )

        update.value = false
        chain.assertReadableState(
            2 to 0,
            0 to 1,
            1 to 0,
        )

        update.value = true
        chain.assertReadableState(
            2 to 0,
            0 to 1,
        )

        update.value = false
        chain.assertReadableState(
            1 to 0,
            2 to 0,
            0 to 1,
        )

        update.value = true
        chain.assertReadableState(
            1 to 0,
            0 to 1,
        )

        update.value = false
        chain.assertReadableState(
            2 to 0,
            1 to 0,
            0 to 1,
        )
    }

}