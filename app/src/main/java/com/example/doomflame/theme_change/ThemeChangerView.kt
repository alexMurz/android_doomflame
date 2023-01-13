package com.example.doomflame.theme_change

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.applyCanvas


sealed interface SnapshotState {
    object Unset : SnapshotState
    object ShouldSet : SnapshotState
    class Set(val bitmap: Bitmap) : SnapshotState
}

class SnapshotStateHolder(
    var state: SnapshotState = SnapshotState.Unset
)

class ThemeChangerView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), AnimatorUpdateListener, Animator.AnimatorListener {

    private var stateHolder: SnapshotStateHolder? = null
    private var state: SnapshotState
        get() = stateHolder?.state ?: SnapshotState.Unset
        set(value) {
            stateHolder?.state = value
        }

    private val animator by lazy {
        ValueAnimator.ofInt(255, 0).apply {
            duration = 750
            addUpdateListener(this@ThemeChangerView)
            addListener(this@ThemeChangerView)
        }
    }

    private val snapshotPaint = Paint()

    fun setStateHolder(stateHolder: SnapshotStateHolder) {
        this.stateHolder = stateHolder
    }

    fun makeSnapshot() {
        if (state != SnapshotState.ShouldSet) {
            if (animator.isRunning) animator.cancel()
            state = SnapshotState.ShouldSet
            invalidate()
        }
    }

    fun animateDrop() {
        if (state is SnapshotState.Set) {
            if (animator.isRunning) animator.cancel()
            animator.start()
        }
    }

    override fun draw(canvas: Canvas?) {
        when (val state = state) {
            SnapshotState.ShouldSet -> {
                println("Draw create snapshot")
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                this.state = SnapshotState.Set(bitmap)
                bitmap.applyCanvas {
                    super.draw(this)
                }
                canvas ?: return
                snapshotPaint.alpha = 0xFF
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, snapshotPaint)
            }
            is SnapshotState.Set -> {
                println("Draw snapshot")
                canvas?.apply {
                    snapshotPaint.alpha = animator.animatedValue as Int
                    super.draw(canvas)
                    drawBitmap(state.bitmap, 0.0f, 0.0f, snapshotPaint)
                }
            }
            else -> {
                println("Draw normally")
                super.draw(canvas)
            }
        }
    }

    //////////////////////////////////////////////////
    // Animation handling logic

    override fun onAnimationUpdate(animation: ValueAnimator) {
        invalidate()
    }

    override fun onAnimationStart(animation: Animator) {
        invalidate()
    }

    override fun onAnimationEnd(animation: Animator) {
        invalidate()
        state = SnapshotState.Unset
    }

    override fun onAnimationCancel(animation: Animator) =
        onAnimationEnd(animation)

    override fun onAnimationRepeat(animation: Animator) {
        throw IllegalStateException("Should be unreachable")
    }
}
