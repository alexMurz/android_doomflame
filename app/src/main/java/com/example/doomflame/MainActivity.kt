package com.example.doomflame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.graphics.createBitmap
import com.example.doomflame.doom_flame.DoomFlameComputeNDK
import com.example.doomflame.doom_flame.DoomFlameView
import com.example.doomflame.swapchain.SwapchainImpl

class MainActivity : AppCompatActivity() {

    private lateinit var doomView: DoomFlameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resolution = 512
        doomView = DoomFlameView.Builder()
            .withResolution(resolution)
            .withSwapchain(SwapchainImpl(3) { createBitmap(resolution, resolution) })
            .withCompute { DoomFlameComputeNDK(it) }
            .build(this)
        setContentView(doomView)
    }

    override fun onResume() {
        super.onResume()
        doomView.resume()
    }

    override fun onPause() {
        super.onPause()
        doomView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        doomView.destroy()
    }
}