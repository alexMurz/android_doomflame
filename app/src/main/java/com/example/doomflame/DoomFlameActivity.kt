package com.example.doomflame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.doomflame.doom_flame.DoomFlameComputeNDK
import com.example.doomflame.doom_flame.DoomFlameView

class DoomFlameActivity : AppCompatActivity() {

    private lateinit var doomView: DoomFlameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resolution = 512
        doomView = DoomFlameView.Builder()
            .withResolution(resolution)
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