package com.example.doomflame.theme_change

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.withClip
import kotlin.math.min

private class ViewState(
    var progress: Float,
    var ox: Float,
    var oy: Float,
)

class SwappingFrameLayout(
    context: Context,
) : FrameLayout(context) {

    private val states = mutableListOf<ViewState>()
    private var timestamp = System.nanoTime()

    private val tempPath = Path()

    fun updateView(view: View) {
        addView(view)
        val progress = if (childCount == 0) 1f else 0f
        states.add(
            ViewState(
                progress = progress,
                ox = 0f,
                oy = 0f
            )
        )
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas ?: return
        val deltaTimeMillis = 1f / 60f

        var i = 0
        while (i < states.size) {
            val state = states[i]
            val view = getChildAt(i)

            val progress = min(state.progress + deltaTimeMillis, 1.0f)
            state.progress = progress

            if (progress < 1.0) tempPath.apply {
                reset()
                val cx = state.ox
                val cy = state.oy
                val rr = 1000
                val r = rr * state.progress
                addOval(cx - r, cy - r, cx + r, cy + r, Path.Direction.CW)
                canvas.withClip(tempPath) {
                    view.draw(canvas)
                }
            } else {
                view.draw(canvas)
            }
            i++
        }

        i = 0
        while (i < states.size - 1) {
            val state = states[i]
            val next = states[i + 1]
            if (next.progress >= 1.0f) {
                states.removeAt(i)
                removeViewAt(i)
            } else {
                i++
            }
        }

        if (states.size > 1 || states[0].progress < 1.0) invalidate()
    }

}

