package com.example.doomflame.theme_change

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.graphics.applyCanvas
import kotlin.math.sqrt

data class SnapshotStateHolder(
    var animation: Int = ANIM_CROSS_FADE,
    var shouldSet: Boolean = false,
    var bitmap: Bitmap? = null,
) : Parcelable, java.io.Serializable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(Bitmap::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(animation)
        parcel.writeByte(if (shouldSet) 1 else 0)
        parcel.writeParcelable(bitmap, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SnapshotStateHolder> {
        override fun createFromParcel(parcel: Parcel): SnapshotStateHolder {
            return SnapshotStateHolder(parcel)
        }

        override fun newArray(size: Int): Array<SnapshotStateHolder?> {
            return arrayOfNulls(size)
        }
    }
}

const val ANIM_CROSS_FADE = 0
const val ANIM_FLIP_OVER = 1
const val ANIM_CIRCLE = 2

class ThemeChangerView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), AnimatorUpdateListener, Animator.AnimatorListener {

    var stateHolder: SnapshotStateHolder? = null

    private val animator by lazy {
        ValueAnimator.ofFloat(1.0f, 0.0f).apply {
            duration = 600
            addUpdateListener(this@ThemeChangerView)
            addListener(this@ThemeChangerView)
        }
    }

    private val snapshotPaint = Paint()

    private var animationProgress = 1f
    private val srcRect = Rect()
    private val dstRect = Rect()
    private val path = Path().apply {
        fillType = Path.FillType.WINDING
    }

    fun makeSnapshot() {
        val h = stateHolder ?: return
        if (!h.shouldSet) {
            if (animator.isRunning) animator.cancel()
            h.shouldSet = true
            invalidate()
        }
    }

    fun animateDrop() {
        val h = stateHolder ?: return
        if (!h.shouldSet && h.bitmap != null) {
            if (animator.isRunning) animator.cancel()
            animator.start()
        }
    }

    override fun draw(canvas: Canvas?) {
        canvas ?: return
        val h = stateHolder

        when {
            h?.shouldSet == true -> {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                h.shouldSet = false
                h.bitmap = bitmap
                bitmap.applyCanvas {
                    super.draw(this)
                }
                snapshotPaint.alpha = 0xFF
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, snapshotPaint)
            }
            h?.bitmap != null -> {
                super.draw(canvas)

                val bitmap = h.bitmap ?: return
                val progress = animationProgress

                when (h.animation) {
                    ANIM_FLIP_OVER -> {
                        srcRect.set(0, 0, (width * progress).toInt(), height)
                        dstRect.set(srcRect)
                        snapshotPaint.alpha = 0xFF
                    }
                    ANIM_CIRCLE -> {
                        srcRect.set(0, 0, width, height)
                        dstRect.set(srcRect)
                        snapshotPaint.alpha = 0xFF
                        val diameter = sqrt((width * width + height * height).toFloat())
                        val radProgress = diameter * 0.5f * progress
                        val cx = width * 0.5f
                        val cy = height * 0.5f
                        path.apply {
                            reset()
                            addOval(
                                cx - radProgress,
                                cy - radProgress,
                                cx + radProgress,
                                cy + radProgress,
                                Path.Direction.CCW,
                            )
                            canvas.clipPath(this)
                        }
                    }
                    else -> {
                        srcRect.set(0, 0, width, height)
                        dstRect.set(srcRect)
                        snapshotPaint.alpha = (progress * 0xFF).toInt()
                    }
                }
                canvas.drawBitmap(bitmap, srcRect, dstRect, snapshotPaint)
            }
            else -> super.draw(canvas)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // No touche-touche while animating
        if (animator.isRunning) return true
        return super.onInterceptTouchEvent(ev)
    }

    //////////////////////////////////////////////////
    // Animation handling logic

    override fun onAnimationUpdate(animation: ValueAnimator) {
        animationProgress = animation.animatedValue as Float
        invalidate()
    }

    override fun onAnimationStart(animation: Animator) {
        animationProgress = 1.0f
        invalidate()
    }

    override fun onAnimationEnd(animation: Animator) {
        animationProgress = 0.0f
        stateHolder?.bitmap = null
        invalidate()
    }

    override fun onAnimationCancel(animation: Animator) =
        onAnimationEnd(animation)

    override fun onAnimationRepeat(animation: Animator) {
        throw IllegalStateException("Should be unreachable")
    }
}
