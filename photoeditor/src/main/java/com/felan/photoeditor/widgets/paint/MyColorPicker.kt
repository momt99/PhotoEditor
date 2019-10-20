package com.felan.photoeditor.widgets.paint

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import androidx.core.util.rangeTo
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.*
import com.google.android.material.math.MathUtils.lerp
import kotlin.math.abs
import kotlin.math.max

class MyColorPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private val COLORS = intArrayOf(
            -0x15d8c7,
            -0x24c52e,
            -0xcfae1d,
            -0xb63a13,
            -0x7f379c,
            -0x3219b,
            -0x369b3,
            -0x1000000,
            -0x1
        )

        private val LOCATIONS =
            floatArrayOf(0.0f, 0.14f, 0.24f, 0.39f, 0.49f, 0.62f, 0.73f, 0.85f, 1.0f)

        const val MATCH_PARENT = -1

        private const val DEFAULT_EXPANSION_DURATION = 200L

        private const val PREFERENCES_NAME = "color_picker"
        private const val LAST_COLOR_LOCATION_PREF_KEY = "last_color_location"
        private const val LAST_WEIGHT_PREF_KEY = "last_weight"

    }

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var barHeight: Int by viewInvalidator(MATCH_PARENT, preInvalidate = { _, _ ->
        updateBarRect(availableWidth, actualBarHeight)
    })

    private val actualBarHeight
        get() = if (barHeight == MATCH_PARENT) availableHeight else barHeight


    private var colorLocation: Float by RangedProperty(
        0f,
        1f,
        viewInvalidator(0f, preInvalidate = { _, value ->
            selectedColor = getColorForLocation(value)
        })
    )

    private var lastLocationIndex = 0

    private fun getColorForLocation(location: Float): Int {
        if (location <= 0f)
            return COLORS[0]
        else if (location >= 1f)
            return COLORS[COLORS.size - 1]

        val searchRange = if (LOCATIONS[lastLocationIndex] < location)
            lastLocationIndex + 1 until LOCATIONS.size
        else
            lastLocationIndex - 1 downTo 0
        val searchUpward = searchRange.first < searchRange.last

        var leftIndex = -1
        var rightIndex = -1

        if (searchUpward) {
            for (i in searchRange)
                if (LOCATIONS[i] > location) {
                    leftIndex = i - 1
                    rightIndex = i
                    break
                }
        } else
            for (i in searchRange)
                if (LOCATIONS[i] < location) {
                    leftIndex = i
                    rightIndex = i + 1
                    break
                }

        val leftLocation = LOCATIONS[leftIndex]
        val rightLocation = LOCATIONS[rightIndex]
        val factor = (location - leftLocation) / (rightLocation - leftLocation)
        return ColorUtils.blendARGB(COLORS[leftIndex], COLORS[rightIndex], factor)
    }

    private fun getLocationForColor(color: Int): Float {
        fun Int.toRGBArray(): IntArray =
            intArrayOf(Color.red(this), Color.green(this), Color.blue(this))

        val color = color.toRGBArray()

        val possiblePairs = MutableList(COLORS.size - 1)
        { i -> Triple(COLORS[i].toRGBArray(), COLORS[i + 1].toRGBArray(), i) }

        fun isBetween(value: IntArray, start: IntArray, end: IntArray): Boolean =
            value[0] in start[0] safeRangeTo end[0] &&
                    value[1] in start[1] safeRangeTo end[1] &&
                    value[2] in start[2] safeRangeTo end[2]
        return possiblePairs.find {
            isBetween(color, it.first, it.second)
        }?.run {
            val component = IntRange(0, 2).find { first[it] != second[it] }!!
            lerp(
                LOCATIONS[third],
                LOCATIONS[third + 1],
                abs((first[component] - color[component]) / (first[component] - second[component]).toFloat())
            )
        } ?: throw IllegalArgumentException("Color is not in supported ranges: $color")
    }

    private val thumbShadow: Drawable? =
        ResourcesCompat.getDrawable(resources, R.drawable.knob_shadow, context.theme)

    private val thumbShadowRadiusDecrease: Float

    private val thumbBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    @get:ColorInt
    @setparam:ColorInt
    var thumbBackgroundColor: Int
        get() = thumbBackgroundPaint.color
        set(value) {
            thumbBackgroundPaint.color = value
            invalidate()
        }

    private val thumbColorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbColorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    @get:ColorInt
    @setparam:ColorInt
    var selectedColor: Int
        get() = thumbColorPaint.color
        private set(value) {
            if (selectedColor == value)
                return
            thumbColorPaint.color = value

            thumbColorStrokePaint.run {
                val hsv = FloatArray(3)
                Color.colorToHSV(value, hsv)
                color = if (hsv[0] < 0.001 && hsv[1] < 0.001 && hsv[2] > 0.92f) {
                    val c = ((1.0f - (hsv[2] - 0.92f) / 0.08f * 0.22f) * 255).toInt()
                    Color.rgb(c, c, c)
                } else
                    value
            }

            invalidate()
            selectedColorChanged(value)
        }

    fun setCurrentColor(@ColorInt color: Int) {
        colorLocation = getLocationForColor(color)
    }

    val selectedColorChanged = EventHandler<@ColorInt Int>()

    var thumbRadius: Float by viewInvalidator(0f, preInvalidate = { _, value ->
        val iv = value.toInt()
        thumbShadow?.setBounds(-iv, -iv, iv, iv)
        updateBarRect(availableWidth, actualBarHeight)
    })

    var thumbColorMaxRadiusFactor: Float by RangedProperty(0f, 1f, viewInvalidator(0.9f))

    var thumbColorMinRadiusFactor: Float by RangedProperty(0f, 1f, viewInvalidator(0.1f))

    var thumbExpansionFactor: Float by viewInvalidator(1.5f)

    private var thumbExpansionProgress: Float by viewInvalidator(0f)

    var thumbExpansionDistance: Float by viewInvalidator(0f)

    private var isChangingWeight: Boolean by viewInvalidator(false)

    var selectedWeight: Float by RangedProperty(
        0f,
        1f,
        viewInvalidator(0.5f, postInvalidate = { _, value ->
            selectedWeightChanged(value)
        })
    )

    val selectedWeightChanged = EventHandler<Float>()

    var thumbWeightChangeDistance: Float by viewInvalidator(0f)

    private val availableWidth
        get() = width - paddingLeft - paddingRight

    private val availableHeight
        get() = height - paddingTop - paddingBottom

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBarRect(availableWidth, actualBarHeight)
        updateWeightChangeArea()
    }

    private val barRect = RectF()
    private fun updateBarRect(availableWidthForBar: Int, barHeight: Int) =
        barRect.apply {
            left = paddingLeft + thumbRadius
            right = left + availableWidthForBar.toFloat() - 2 * thumbRadius
            bottom = (this@MyColorPicker.height - paddingBottom -
                    max(0f, thumbRadius - (barHeight / 2)))
            top = bottom - barHeight
            updateGradientShader(left, right)
        }

    private val weightChangeArea = RectF()
    private fun updateWeightChangeArea() =
        weightChangeArea.apply {
            val thumbRadius = thumbRadius * thumbExpansionFactor
            top = paddingTop.toFloat() + thumbRadius + thumbExpansionDistance
            bottom =
                (barRect.top + barRect.bottom) / 2f - thumbWeightChangeDistance
            left = barRect.left
            right = barRect.right
        }

    private fun updateGradientShader(left: Float, right: Float) {
        gradientPaint.shader = LinearGradient(
            left, 0f, right, 0f,
            COLORS, LOCATIONS, Shader.TileMode.CLAMP
        )
    }

    private var disableInvalidation: Boolean = false


    init {
        disableInvalidation = true
        thumbShadowRadiusDecrease = resources.displayMetrics.density * 1    //2dp

        context.obtainStyledAttributes(
            attrs,
            R.styleable.MyColorPicker,
            R.attr.colorPickerStyle,
            R.style.MyColorPickerDefaultStyle
        ).apply {
            barHeight = getDimensionPixelSize(R.styleable.MyColorPicker_barHeight, -1)
            thumbBackgroundColor =
                getColor(R.styleable.MyColorPicker_thumbBackgroundColor, Color.WHITE)
            thumbRadius = getDimensionOrThrow(R.styleable.MyColorPicker_thumbRadius)
            thumbColorMaxRadiusFactor =
                getFloat(R.styleable.MyColorPicker_thumbColorMaxRadiusFactor, 0.9f)
            thumbColorMinRadiusFactor =
                getFloat(R.styleable.MyColorPicker_thumbColorMinRadiusFactor, 0.1f)
            thumbExpansionFactor = getFloat(R.styleable.MyColorPicker_thumbExpansionFactor, 1.5f)
            thumbExpansionDistance =
                getDimensionOrThrow(R.styleable.MyColorPicker_thumbExpansionDistance)
            thumbWeightChangeDistance =
                getDimensionOrThrow(R.styleable.MyColorPicker_thumbWeightChangeDistance)
        }.also { it.recycle() }

        loadSettings()
        disableInvalidation = false
    }

    private var isDragging = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1)
            return false

        disableInvalidation = true

        try {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val thumbRect = RectF().apply {
                        val thumbCenter = PointF(
                            lerp(barRect.left, barRect.right, colorLocation),
                            (barRect.top + barRect.bottom) / 2f
                        )
                        set(
                            thumbCenter.x - thumbRadius,
                            thumbCenter.y - thumbRadius,
                            thumbCenter.x + thumbRadius,
                            thumbCenter.y + thumbRadius
                        )
                    }
                    isDragging =
                        barRect.contains(event.x, event.y) || thumbRect.contains(event.x, event.y)
                    if (!isDragging)
                        return false
                    colorLocation = (event.x - barRect.left) / barRect.width()
                    isChangingWeight = false
                    startExpansionAnimation(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging)
                        return false
                    colorLocation = (event.x - barRect.left) / barRect.width()
                    isChangingWeight = event.y < weightChangeArea.bottom
                    if (isChangingWeight)
                        selectedWeight =
                            (weightChangeArea.bottom - event.y) / weightChangeArea.height()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    startExpansionAnimation(false)
                }
            }
        } finally {
            disableInvalidation = false
            invalidate()
        }

        return true
    }

    private var expansionProgressAnimator: ValueAnimator? = null
    private var expansionProgressInterpolator: Interpolator = OvershootInterpolator(1.1f)

    private fun startExpansionAnimation(expand: Boolean) {
        expansionProgressAnimator?.cancel()
        expansionProgressAnimator =
            ValueAnimator.ofFloat(*if (expand) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f))
                .apply {
                    addUpdateListener { thumbExpansionProgress = it.animatedValue as Float }
                    interpolator = expansionProgressInterpolator
                    duration = DEFAULT_EXPANSION_DURATION
                }.apply { start() }
    }


    override fun invalidate() {
        if (!disableInvalidation)
            super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        //Bar
        canvas.drawRoundRect(barRect, barRect.height() / 2, barRect.height() / 2, gradientPaint)

        //Thumb
        canvas.withTranslation(
            lerp(barRect.left, barRect.right, colorLocation),
            barRect.bottom - barRect.height() / 2 -
                    thumbExpansionProgress *
                    (thumbExpansionDistance + if (isChangingWeight) weightChangeArea.height() * selectedWeight else 0f)
        ) {
            val scaleFactor = lerp(1f, thumbExpansionFactor, thumbExpansionProgress)
            withScale(x = scaleFactor, y = scaleFactor) {
                thumbShadow?.draw(canvas)
                canvas.drawCircle(
                    0f, 0f,
                    thumbRadius - thumbShadowRadiusDecrease,
                    thumbBackgroundPaint
                )
                val r = (thumbRadius - thumbShadowRadiusDecrease) *
                        lerp(thumbColorMinRadiusFactor, thumbColorMaxRadiusFactor, selectedWeight)
                canvas.drawCircle(0f, 0f, r, thumbColorPaint)
                canvas.drawCircle(0f, 0f, r, thumbColorStrokePaint)
            }
        }
    }

    fun loadSettings() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).run {
            colorLocation = getFloat(LAST_COLOR_LOCATION_PREF_KEY, 0f)
            selectedWeight = getFloat(LAST_WEIGHT_PREF_KEY, 0.5f)
        }
    }

    fun saveSettings(commit: Boolean = false) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit(commit) {
            putFloat(LAST_COLOR_LOCATION_PREF_KEY, colorLocation)
            putFloat(LAST_WEIGHT_PREF_KEY, selectedWeight)
        }
}