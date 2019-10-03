package com.felan.photoeditor.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.getBooleanOrThrow
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getFloatOrThrow
import com.felan.photoeditor.R

import com.felan.photoeditor.utils.EventHandler
import com.felan.photoeditor.utils.RangedProperty
import com.felan.photoeditor.utils.ViewInvalidatorProperty
import kotlin.math.max


class MySeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var barHeight: Float = 0f

    private val emptyBarPaint = Paint()
    var emptyBarAlpha: Float
        get() = emptyBarPaint.alpha / 255f
        set(value) {
            emptyBarPaint.alpha = (value.coerceIn(0f, 1f) * 255).toInt()
            invalidate()
        }

    @get:ColorInt
    @setparam:ColorInt
    var emptyBarColor: Int
        get() = emptyBarPaint.color
        set(value) {
            emptyBarPaint.color =
                (emptyBarPaint.color and 0xFF000000.toInt()) or (value and 0x00FFFFFF)
            invalidate()
        }

    private val filledBarPaint = Paint()
    @get:ColorInt
    @setparam:ColorInt
    var filledBarColor: Int
        get() = filledBarPaint.color
        set(value) {
            filledBarPaint.color = value
            invalidate()
        }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    @get:ColorInt
    @setparam:ColorInt
    var thumbColor: Int
        get() = thumbPaint.color
        set(value) {
            thumbPaint.color = value
        }

    fun setAllColors(@ColorInt color: Int) {
        emptyBarColor = color
        filledBarColor = color
        thumbColor = color
    }

    var thumbSize: Float by ViewInvalidatorProperty(0f)

    var enableMiddlePoint: Boolean by ViewInvalidatorProperty(false)

    var middlePointerThickness: Float by ViewInvalidatorProperty(0f)

    var touchRadiusThreshold: Float = thumbSize

    var min: Float = 0f

    var max: Float = 100f

    var progress: Float
        get() = rawProgress * (max - min) + min
        set(value) {
            rawProgress = (value.coerceIn(min, max) - min) / (max - min)
        }

    data class ProgressChangedEventArgs(val rawProgress: Float, val progress: Float)

    val progressChanged = EventHandler<ProgressChangedEventArgs>()
    var rawProgress: Float by RangedProperty(0f, 1f, ViewInvalidatorProperty(0f, invalidator = {
        it.invalidate()
        progressChanged(ProgressChangedEventArgs(rawProgress, progress))
    }))

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.MySeekBar,
            R.attr.mysSeekBarStyle,
            R.style.MySeekBarDefaultStyle
        )

        barHeight = a.getDimensionOrThrow(R.styleable.MySeekBar_barHeight)
        thumbSize = a.getDimensionOrThrow(R.styleable.MySeekBar_thumbSize)

        emptyBarColor = a.getColorOrThrow(R.styleable.MySeekBar_emptyBarColor)
        emptyBarAlpha = a.getFloatOrThrow(R.styleable.MySeekBar_emptyBarAlpha)
        filledBarColor = a.getColorOrThrow(R.styleable.MySeekBar_filledBarColor)
        thumbColor = a.getColorOrThrow(R.styleable.MySeekBar_thumbColor)

        enableMiddlePoint = a.getBooleanOrThrow(R.styleable.MySeekBar_enableMidPoint)
        middlePointerThickness = a.getDimensionOrThrow(R.styleable.MySeekBar_middlePointerThickness)

        touchRadiusThreshold = a.getDimensionOrThrow(R.styleable.MySeekBar_touchRadiusThreshold)

        min = a.getFloatOrThrow(R.styleable.MySeekBar_min)
        max = a.getFloatOrThrow(R.styleable.MySeekBar_max)

        a.recycle()
    }

    private val availableProgressWidth
        get() = width - thumbSize - paddingLeft - paddingRight

    private fun calcThumbCenterX() = (availableProgressWidth * rawProgress) + thumbSize / 2

    private var startX: Float = -1f
    private var startProgress: Float = 0f

    val pressedChanged = EventHandler<Boolean>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return false

        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val threshold = max(thumbSize, touchRadiusThreshold)
                val thumbX = calcThumbCenterX()
                if (thumbX - threshold <= x && x <= thumbX + threshold &&
                    height / 2 - threshold <= y && y <= height / 2 + threshold
                ) {
                    isPressed = true
                    pressedChanged(true)
                    startX = x
                    startProgress = rawProgress
                    parent.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isPressed) return false

                isPressed = false
                pressedChanged(false)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isPressed) return false
                rawProgress = (x - startX) / availableProgressWidth + startProgress
                invalidate()
                return true
            }
        }
        return false
    }

    private val emptyRectF = RectF()
    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        //Empty bar
        emptyRectF.apply {
            set(
                thumbSize / 2,
                height / 2 - barHeight / 2,
                width - thumbSize / 2,
                height / 2 + barHeight
            )
        }.also {
            canvas.drawRoundRect(it, barHeight / 2, barHeight / 2, emptyBarPaint)
        }

        if (!enableMiddlePoint) {
            //Filled bar
            drawProgressRect(
                canvas,
                thumbSize / 2,
                thumbSize / 2 + availableProgressWidth * rawProgress
            )
        } else {
            //Middle pointer
            canvas.drawRect(
                width / 2 - middlePointerThickness,
                (height - thumbSize) / 2,
                (width / 2).toFloat(),
                (height + thumbSize) / 2,
                filledBarPaint
            )

            //Filled bar
            drawProgressRect(
                canvas,
                width / 2f,
                thumbSize / 2 + availableProgressWidth * rawProgress
            )
        }
        //Thumb
        canvas.drawCircle(
            thumbSize / 2 + availableProgressWidth * rawProgress,
            height / 2f,
            thumbSize / 2,
            thumbPaint
        )

        canvas.restore()
    }

    private val filledRectF = RectF()
    private fun drawProgressRect(canvas: Canvas, start: Float, end: Float) =
        filledRectF.apply {
            set(
                start,
                height / 2 - barHeight / 2,
                end,
                height / 2 + barHeight
            )
        }.also {
            canvas.drawRoundRect(filledRectF, barHeight / 2, barHeight / 2, filledBarPaint)
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        @Suppress("NAME_SHADOWING")
        var heightMeasureSpec = heightMeasureSpec
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                (thumbSize + paddingTop + paddingBottom).toInt(),
                MeasureSpec.EXACTLY
            )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
