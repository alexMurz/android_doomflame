package com.example.doomflame.doom_flame

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.graphics.createBitmap
import com.example.doomflame.math.WindowMean
import com.example.doomflame.swapchain.DoubleBufferingSwapchain
import com.example.doomflame.swapchain.Swapchain
import kotlin.system.measureTimeMillis

private class UpdaterThread(
    private val swapchain: Swapchain<Bitmap>,
    private val compute: DoomFlameCompute,
    private val upsMean: WindowMean? = null,
) : Thread("UpdaterThread"), Swapchain.Updater<Bitmap> {

    override fun update(value: Bitmap) {
        val millis = measureTimeMillis {
            compute.draw(value)
        }
        upsMean?.put(millis / 1e3f)
    }

    override fun run() {
        while (!isInterrupted) {
            swapchain.update(this)
        }
    }
}

@SuppressLint("ViewConstructor")
class DoomFlameView private constructor(
    context: Context,
    private val swapchain: Swapchain<Bitmap>,
    private val resolution: Int,
    private val compute: DoomFlameCompute,
) : View(context) {

    private val upsMean = WindowMean(100)
    private val srcRect = Rect(0, 0, resolution, resolution)
    private val dstRect = Rect(0, 0, 0, 0)

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private val upsPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 36f
    }

    private var updaterThread: Thread? = null

    fun resume() {
        pause()
        updaterThread = UpdaterThread(swapchain, compute, upsMean).apply {
            start()
        }
        invalidate()
    }

    fun pause() {
        updaterThread?.interrupt()
        updaterThread = null
    }

    fun destroy() {
        pause()
        compute.dispose()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        val upsText = String.format(
            "${compute} UPS: %.2f P90: %.2f",
            1 / upsMean.average,
            1 / upsMean.percentile(0.90f)
        )

        canvas.drawColor(Color.WHITE)

        canvas.drawText(
            upsText,
            50f,
            height.toFloat() - 50,
            upsPaint
        )

        val viewHeight = height
        dstRect.apply {
            val dWidth = width
            val dHeight = dWidth
            val dTop = (viewHeight - dHeight) / 2

            right = dWidth
            top = dTop
            bottom = dTop + dHeight
        }

        swapchain.consume {
            canvas.drawBitmap(it, srcRect, dstRect, paint)
        }

        if (updaterThread?.isInterrupted == false) {
            invalidate()
        }
    }

    class Builder {
        private var swapchain: Swapchain<Bitmap>? = null
        private var resolution: Int = 320
        private var computeFactory: ((Int) -> DoomFlameCompute)? = null

        fun withSwapchain(swapchain: Swapchain<Bitmap>) = apply {
            this.swapchain = swapchain
        }

        fun withResolution(resolution: Int) = apply {
            this.resolution = resolution
        }

        fun withCompute(computeFactory: (resolution: Int) -> DoomFlameCompute) = apply {
            this.computeFactory = computeFactory
        }

        fun build(context: Context): DoomFlameView {
            assert(resolution > 0)

            val compute = computeFactory?.invoke(resolution)
                ?: DoomFlameComputeCPU(resolution)

            val swapchain = swapchain
                ?: DoubleBufferingSwapchain {
                    createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888)
                }

            return DoomFlameView(
                context = context,
                swapchain = swapchain,
                resolution = resolution,
                compute = compute,
            )
        }
    }
}
