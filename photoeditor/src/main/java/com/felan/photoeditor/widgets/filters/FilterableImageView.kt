package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.RangedProperty
import com.felan.photoeditor.utils.ReplayEventHandler
import com.felan.photoeditor.utils.SizeX
import kotlin.math.PI
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FilterableImageView @kotlin.jvm.JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    //region Adjust values

    private val enhanceValue_delegate = AdjustParamProperty(
        0f,
        0f,
        100f,
        R.string.label_enhance
    ) //0 100

    var enhanceValue: Float by enhanceValue_delegate


    private val exposureValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_exposure
    )
    var exposureValue: Float by exposureValue_delegate //-100 100


    private val contrastValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_contrast
    )
    var contrastValue: Float by contrastValue_delegate //-100 100


    private val warmthValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_warmth
    )
    var warmthValue: Float by warmthValue_delegate //-100 100


    private val saturationValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_saturation
    )
    var saturationValue: Float by saturationValue_delegate //-100 100


    val fadeValue_delegate = AdjustParamProperty(
        0f,
        0f,
        100f,
        R.string.label_fade
    )
    var fadeValue: Float by fadeValue_delegate // 0 100


    val tintShadowColor_delegate = AdjustParamProperty(
        Color.TRANSPARENT,
        Int.MIN_VALUE,
        Int.MAX_VALUE
    )
    @get: ColorInt
    var tintShadowsColor: Int by tintShadowColor_delegate //0 0xffffffff


    val tintHighlightColor_delegate = AdjustParamProperty(
        Color.TRANSPARENT,
        Int.MIN_VALUE,
        Int.MAX_VALUE
    )
    @get: ColorInt
    var tintHighlightsColor: Int by tintHighlightColor_delegate //0 0xffffffff


    val highlightsValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_highlights
    )
    var highlightsValue: Float by highlightsValue_delegate //-100 100


    private val shadowsValue_delegate = AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_shadows
    )
    var shadowsValue: Float by shadowsValue_delegate //-100 100


    private val vignetteValue_delegate = AdjustParamProperty(
        0f,
        0f,
        100f,
        R.string.label_vignette
    )
    var vignetteValue: Float by vignetteValue_delegate //0 100


    val grainValue_delegate = AdjustParamProperty(
        0f,
        0f,
        100f,
        R.string.label_grain
    )
    var grainValue: Float by grainValue_delegate //0 100


    val sharpenValue_delegate = AdjustParamProperty(
        0f,
        0f,
        100f,
        R.string.label_sharpen
    )
    var sharpenValue: Float by sharpenValue_delegate //0 100


    //endregion

    //region Blur values

    var blurType: BlurType by RequestRenderProperty(
        BlurType.NONE
    )

    var blurExcludeSize: Float by RequestRenderProperty(0.35f)

    var blurExcludePoint: PointF by RequestRenderProperty(
        PointF(0.5f, 0.5f)
    )

    var blurExcludeBlurSize: Float by RequestRenderProperty(
        0.15f
    )

    var blurAngle: Float by RequestRenderProperty(PI.toFloat() / 2.0f)

    //endregion

    //region Curve values

    var curvesToolValue: CurvesToolValue =
        CurvesToolValue()

    //endregion

    private val textureView: TextureView

    private val textureContainer: FrameLayout

    private val layoutChangeListener = object : OnLayoutChangeListener {
        private var lastWidth = -1
        private var lastHeight = -1

        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            val newWidth = right - left
            val newHeight = bottom - top
            if (newWidth != lastWidth || newHeight != lastHeight) {
                updateTextureViewSize(newWidth, newHeight)
                lastWidth = newWidth
                lastHeight = newHeight
            }
        }
    }

    init {
        textureView = TextureView(context).apply {
            surfaceTextureListener = this@FilterableImageView
        }
        textureContainer = FrameLayout(context).apply {
            addView(
                textureView,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
            addOnLayoutChangeListener(layoutChangeListener)
        }
        addView(
            textureContainer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    //region Texture Callbacks
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        textureSize = SizeX(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        eglThread?.shutdown()
        eglThread = null

        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        if (eglThread != null) return

        textureSize = SizeX(width, height)
        setupEGLThread(surface, image)
    }
    //endregion

    private var eglThread: TelegramEGLThread? = null

    var image: Bitmap? = null
        get
        set(value) {
            field = value
            updateTextureViewSize(textureContainer.width, textureContainer.height)
            setupEGLThread(textureView.surfaceTexture, value)
        }

    private var textureSize: SizeX = SizeX(0, 0)
        set(value) {
            field = value
            eglThread?.apply {
                setSurfaceTextureSize(value.width, value.height)
                requestRender(false, true)
                postRunnable { eglThread?.requestRender(false, true) }
            }
        }

    private fun setupEGLThread(surface: SurfaceTexture?, image: Bitmap?) {
        if (surface == null || image == null) return
        eglThread = TelegramEGLThread(surface, image, this).apply {
            setSurfaceTextureSize(textureSize.width, textureSize.height)
            requestRender(true, true)
        }
    }

    val textureSizeChanged = ReplayEventHandler<SizeX>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTextureViewSize(w, h)
    }

    private fun updateTextureViewSize(width: Int, height: Int) {
        image?.let { img ->
            textureView.updateLayoutParams {
                val scaleX = width.toFloat() / img.width
                val scaleY = height.toFloat() / img.height
                if (scaleX > scaleY) {
                    this.height = height
                    this.width = (img.width * scaleY).toInt()
                } else {
                    this.width = width
                    this.height = (img.height * scaleX).toInt()
                }
            }
            textureSizeChanged(textureViewSize)
        }
    }

    val textureViewSize
        get() = SizeX(textureView.layoutParams.width, textureView.layoutParams.height)


    private var requestRenderEnabled = true

    fun requestRender() {
        if (requestRenderEnabled)
            eglThread?.requestRender(false)
    }

    fun updateGrouped(transaction: FilterableImageView.() -> Unit) {
        requestRenderEnabled = false
        transaction(this)
        requestRenderEnabled = true
        requestRender()
    }

    val resultBitmap: Bitmap?
        get() = if (eglThread != null) eglThread!!.texture else null

    //We do this only for the huge overhead of reflection
    fun getAllPropertyDelegates(): Array<AdjustParamProperty<Float>> {
        return arrayOf(
            enhanceValue_delegate,
            exposureValue_delegate,
            contrastValue_delegate,
            warmthValue_delegate,
            saturationValue_delegate,
            fadeValue_delegate,
            vignetteValue_delegate,
            grainValue_delegate,
            sharpenValue_delegate,
            shadowsValue_delegate,
            highlightsValue_delegate
        )
    }
}

class AdjustParamProperty<T : Comparable<T>>(
    initialValue: T,
    minValue: T,
    maxValue: T,
    val labelResource: Int = 0
) : RangedProperty<FilterableImageView, T>(
    minValue, maxValue,
    RequestRenderProperty(initialValue)
)

open class RequestRenderProperty<T>(initialValue: T) :
    ReadWriteProperty<FilterableImageView, T> {
    private var value: T = initialValue

    fun getValue(): T = value

    override fun getValue(thisRef: FilterableImageView, property: KProperty<*>): T = getValue()

    fun setValue(thisRef: FilterableImageView, value: T) {
        if (value == this.value)
            return

        this.value = value
        thisRef.requestRender()
    }

    override fun setValue(thisRef: FilterableImageView, property: KProperty<*>, value: T) =
        setValue(thisRef, value)

}
