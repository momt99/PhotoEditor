package com.felan.photoeditor.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getFloatOrThrow
import androidx.core.content.res.getIntOrThrow
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.EventHandler
import com.felan.photoeditor.utils.RangedProperty
import com.felan.photoeditor.utils.ViewInvalidatorProperty
import com.felan.photoeditor.utils.viewInvalidator
import java.lang.Exception
import kotlin.math.*

class RotationWheel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val screenDensity = resources.displayMetrics.density

    private val primaryPaint: Paint = Paint()
    private val secondaryPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @get:ColorInt
    @setparam:ColorInt
    var primaryColor: Int
        get() = primaryPaint.color
        set(value) {
            primaryPaint.color = value
            invalidate()
        }

    @get:ColorInt
    @setparam:ColorInt
    var secondaryColor: Int
        get() = secondaryPaint.color
        set(value) {
            secondaryPaint.color = value
            invalidate()
        }

    private val cursorRect = RectF()

    var centerLineThickness: Float by ViewInvalidatorProperty(0f)
    var otherLinesThickness: Float by ViewInvalidatorProperty(0f)
    var cursorLineThickness: Float by viewInvalidator(0f, preInvalidate = { _, _ ->
        updateCursorRect(width, height)
    })

    var centerLineHeight: Float = -1f
        get
        set(value) {
            field = value
            if (value == MATCH_PARENT)
                updateLineHeights()
            else
                internalCenterLineHeight = value
        }

    var otherLinesHeight: Float = -1f
        get
        set(value) {
            field = value
            if (value == MATCH_PARENT)
                updateLineHeights()
            else
                internalOtherLinesHeight = value
        }

    var cursorLineHeight: Float = -1f
        get
        set(value) {
            field = value
            if (value == MATCH_PARENT)
                updateLineHeights()
            else
                internalCursorLineHeight = value
        }

    private var internalCenterLineHeight: Float by ViewInvalidatorProperty(0f)
    private var internalOtherLinesHeight: Float by ViewInvalidatorProperty(0f)
    private var internalCursorLineHeight: Float by viewInvalidator(0f, preInvalidate = { _, _ ->
        updateCursorRect(width, height)
    })

    val rotationStarted = EventHandler<RotationWheel>()
    val rotationEnded = EventHandler<RotationWheel>()

    val valueChanged = EventHandler<Float>()

    var value: Float by RangedProperty(
        -360f,
        360f,
        viewInvalidator(0f, postInvalidate = { _, value -> valueChanged(value) })
    )

    var selectRangeMin by RangedProperty(-360f, 360f, 0f)
    var selectRangeMax by RangedProperty(-360f, 360f, 0f)

    var unrealityScale by RangedProperty(0f, Float.MAX_VALUE, ViewInvalidatorProperty(1f))

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.RotationWheel,
            R.attr.rotationWheelStyle,
            R.style.RotationWheelDefaultStyle
        )

        primaryColor = a.getColorOrThrow(R.styleable.RotationWheel_primaryColor)
        secondaryColor = a.getColorOrThrow(R.styleable.RotationWheel_secondaryColor)

        centerLineThickness = a.getDimensionOrThrow(R.styleable.RotationWheel_centerLineThickness)
        otherLinesThickness = a.getDimensionOrThrow(R.styleable.RotationWheel_otherLinesThickness)
        cursorLineThickness = a.getDimensionOrThrow(R.styleable.RotationWheel_cursorLineThickness)

        centerLineHeight = try {
            a.getDimensionOrThrow(R.styleable.RotationWheel_centerLineHeight)
        } catch (ex: Exception) {
            a.getIntOrThrow(R.styleable.RotationWheel_centerLineHeight).toFloat()
        }
        otherLinesHeight = try {
            a.getDimensionOrThrow(R.styleable.RotationWheel_otherLinesHeight)
        } catch (ex: Exception) {
            a.getIntOrThrow(R.styleable.RotationWheel_otherLinesHeight).toFloat()
        }
        cursorLineHeight = try {
            a.getDimensionOrThrow(R.styleable.RotationWheel_cursorLineHeight)
        } catch (ex: Exception) {
            a.getIntOrThrow(R.styleable.RotationWheel_cursorLineHeight).toFloat()
        }

        selectRangeMin = a.getFloatOrThrow(R.styleable.RotationWheel_selectRangeMin)
        selectRangeMax = a.getFloatOrThrow(R.styleable.RotationWheel_selectRangeMax)

        unrealityScale = a.getFloat(R.styleable.RotationWheel_unrealityScale, 1f)

        a.recycle()
    }

    private var prevX: Float = 0f
    private var startAngle: Double = 0.0
    private var startValue: Float = 0f

    private val outOfBoundTouchSimilarAngle = Math.toRadians(5.0)
    private val outOfBoundTouchFactor = sin(outOfBoundTouchSimilarAngle)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = ev.x
                startAngle =
                    asin((((ev.x - width / 2.0) / (availableWidth / 2.0)).coerceIn(-1.0, 1.0))) /
                            unrealityScale

                startValue = this.value
                rotationStarted(this)
            }

            MotionEvent.ACTION_MOVE -> {
                val rest = when {
                    ev.x < 0 -> ev.x
                    ev.x > width - paddingRight -> ev.x - (width - paddingRight)
                    else -> 0f
                }

                val currentAngle =
                    (asin((((ev.x - width / 2.0) / (availableWidth / 2.0)).coerceIn(-1.0, 1.0))) +
                            rest / (outOfBoundTouchFactor * availableWidth / 2) * outOfBoundTouchSimilarAngle) /
                            unrealityScale

                this.value =
                    (startValue + Math.toDegrees(currentAngle - startAngle).toFloat())
                        .coerceIn(selectRangeMin, selectRangeMax)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> rotationEnded(this)
        }

        return true
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLineHeights(h)
        updateCursorRect(w, h)
    }

    private fun updateLineHeights(height: Int = this.height) {
        if (centerLineHeight == MATCH_PARENT)
            internalCenterLineHeight = CENTER_LINE_HEIGHT_FACTOR * height

        if (otherLinesHeight == MATCH_PARENT)
            internalOtherLinesHeight = OTHER_LINES_HEIGHT_FACTOR * height

        if (cursorLineHeight == MATCH_PARENT)
            internalCursorLineHeight = CURSOR_LINE_HEIGHT_FACTOR * height
    }


    private fun updateCursorRect(width: Int = this.width, height: Int = this.height) =
        with(cursorRect) {
            left = (width - cursorLineThickness) / 2
            top = (height - internalCursorLineHeight) / 2
            right = (width + cursorLineThickness) / 2
            bottom = (height + internalCursorLineHeight) / 2
        }

    private val availableWidth
        get() = width - paddingLeft - paddingRight

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

        val width = availableWidth
        val height = this.height - paddingTop - paddingBottom

        val angle = value * unrealityScale
        val delta = angle % DELTA_ANGLE
        val segments = floor((angle / DELTA_ANGLE).toDouble()).toInt()

        for (i in 0..LINE_COUNT) {
            var paint = primaryPaint
            var a = i
            if (a <= segments || a == 0 && delta < 0)
                paint = secondaryPaint

            drawLine(
                canvas,
                a,
                delta,
                width,
                height,
                a == segments || a == 0 && segments == -1,
                paint
            )

            if (i != 0) {
                a = -i
                paint = if (a > segments || a == segments + 1) secondaryPaint else primaryPaint
                drawLine(canvas, a, delta, width, height, a == segments + 1, paint)
            }
        }

        secondaryPaint.alpha = 255

        canvas.drawRoundRect(
            cursorRect,
            cursorLineThickness / 2,
            cursorLineThickness / 2,
            secondaryPaint
        )

        canvas.restore()
    }

    private fun drawLine(
        canvas: Canvas, i: Int, delta: Float,
        width: Int, height: Int, isCenter: Boolean,
        paint: Paint
    ) {
        val radius = width / 2f

        val angle = i * DELTA_ANGLE + delta
        val distance = (radius * sin(Math.toRadians(angle.toDouble()))).toFloat()
        val x = width / 2 + distance

        val factor = 1f - (abs(distance) / radius).pow(2f)
        val alpha = min(255, max(0, (factor * 255).toInt()))

        paint.alpha = alpha

        val w = (if (isCenter) centerLineThickness else otherLinesThickness)
        val h = if (isCenter) internalCenterLineHeight else internalOtherLinesHeight

        canvas.drawRect(
            x - w / 2,
            (height - h) / 2,
            x + w / 2,
            (height + h) / 2,
            paint
        )
    }

    companion object {
        const val MATCH_PARENT = -1f
        private const val CENTER_LINE_HEIGHT_FACTOR = 0.8f
        private const val OTHER_LINES_HEIGHT_FACTOR = 0.7f
        private const val CURSOR_LINE_HEIGHT_FACTOR = 1f

        private const val DELTA_ANGLE = 5
        private const val LINE_COUNT = 90 / DELTA_ANGLE

        private const val EPSILON_ANGLE = 0.5f
    }
}
