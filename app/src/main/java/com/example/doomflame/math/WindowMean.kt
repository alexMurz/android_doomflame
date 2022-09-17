package com.example.doomflame.math

class WindowMean(windowSize: Int) {
    private val arr = FloatArray(windowSize)
    private var cursor = 0

    val average: Double
        get() = arr.average()

    fun percentile(p: Float): Double {
        return arr.asSequence().sortedDescending().take((arr.size * (1f - p)).toInt()).average()
    }

    fun put(value: Float) {
        arr[cursor] = value
        cursor = (cursor + 1) % arr.size
    }
}
