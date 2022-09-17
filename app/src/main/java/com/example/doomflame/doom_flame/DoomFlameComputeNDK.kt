package com.example.doomflame.doom_flame

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicLong

private const val NULL_PTR = 0L

class DoomFlameComputeNDK(resolution: Int) : DoomFlameCompute {

    private var ptr = AtomicLong(
        DoomFlameNDKBindings.create(resolution)
    )

    override fun draw(bitmap: Bitmap) {
        DoomFlameNDKBindings.update(ptr.get(), bitmap)
    }

    override fun dispose() {
        val ptr = ptr.getAndSet(NULL_PTR)
        DoomFlameNDKBindings.destroy(ptr)
    }

    override fun toString(): String = "RustCompute(CPU)"
}

object DoomFlameNDKBindings {
    init {
        System.loadLibrary("flamenative")
    }

    external fun create(resolution: Int): Long

    external fun update(ptr: Long, dst: Bitmap)

    external fun destroy(ptr: Long)
}