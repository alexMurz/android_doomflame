package com.example.doomflame.doom_flame

import android.graphics.Bitmap

interface DoomFlameCompute {
    fun draw(bitmap: Bitmap)

    fun dispose() {}
}