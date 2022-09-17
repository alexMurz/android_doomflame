package com.example.doomflame.doom_flame

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.random.Random

private val firePalette = intArrayOf(
    -0xf8f8f9,
    -0xe0f8f9,
    -0xd0f0f9,
    -0xb8f0f9,
    -0xa8e8f9,
    -0x98e0f9,
    -0x88e0f9,
    -0x70d8f9,
    -0x60d0f9,
    -0x50c0f9,
    -0x40b8f9,
    -0x38b8f9,
    -0x20b0f9,
    -0x20a8f9,
    -0x20a8f9,
    -0x28a0f9,
    -0x28a0f9,
    -0x2898f1,
    -0x3090f1,
    -0x3088f1,
    -0x3080f1,
    -0x3078e9,
    -0x3878e9,
    -0x3870e9,
    -0x3868e1,
    -0x4060e1,
    -0x4060e1,
    -0x4058d9,
    -0x4058d9,
    -0x4050d1,
    -0x4850d1,
    -0x4848d1,
    -0x4848c9,
    -0x303091,
    -0x202061,
    -0x101039,
    -0x1,
)

class DoomFlameComputeCPU(
    private val resolution: Int
) : DoomFlameCompute {

    private val rand = Random(System.currentTimeMillis())

    private val tempMap = Array(resolution) {
        val filler = if (it == resolution - 1) firePalette.size - 1 else 0
        IntArray(resolution) { filler }
    }

    override fun draw(bitmap: Bitmap) {
        assert(bitmap.width == resolution && bitmap.height == resolution)

        for (x in tempMap.indices) for (y in 0 until tempMap.size - 1) {
            val dx = rand.nextInt(3) - 1
            val dy = rand.nextInt(6)
            val dt = rand.nextInt(2)

            val x1 = (x + dx).coerceIn(0, tempMap[y].size - 1)
            val y1 = (y + dy).coerceIn(0, tempMap.size - 1)

            val temp = max(0, tempMap[y1][x1] - dt)
            tempMap[y][x] = temp

            bitmap.setPixel(x, y, firePalette[temp])
        }
    }

    override fun toString(): String = "KotlinCompute(SingleThread)"
}