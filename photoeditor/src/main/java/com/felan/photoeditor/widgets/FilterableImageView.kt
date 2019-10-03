package com.felan.photoeditor.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.EventHandler
import com.felan.photoeditor.utils.RangedProperty
import com.felan.photoeditor.utils.SizeFX
import com.felan.photoeditor.utils.SizeX
import kotlin.math.PI
import kotlin.math.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FilterableImageView @kotlin.jvm.JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    //region Adjust values

    var enhanceValue: Float by AdjustParamProperty(0f, 0f, 100f, R.string.label_enhance) //0 100


    var exposureValue: Float by AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_exposure
    ) //-100 100


    var contrastValue: Float by AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_contrast
    ) //-100 100


    var warmthValue: Float by AdjustParamProperty(0f, -100f, 100f, R.string.label_warmth) //-100 100


    var saturationValue: Float by AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_saturation
    ) //-100 100


    var fadeValue: Float by AdjustParamProperty(0f, 0f, 100f, R.string.label_fade) // 0 100


    @get: ColorInt
    var tintShadowsColor: Int by AdjustParamProperty(
        Color.TRANSPARENT,
        Int.MIN_VALUE,
        Int.MAX_VALUE
    ) //0 0xffffffff


    @get: ColorInt
    var tintHighlightsColor: Int by AdjustParamProperty(
        Color.TRANSPARENT,
        Int.MIN_VALUE,
        Int.MAX_VALUE
    ) //0 0xffffffff


    var highlightsValue: Float by AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_highlights
    ) //-100 100


    var shadowsValue: Float by AdjustParamProperty(
        0f,
        -100f,
        100f,
        R.string.label_shadows
    ) //-100 100


    var vignetteValue: Float by AdjustParamProperty(0f, 0f, 100f, R.string.label_vignette) //0 100


    var grainValue: Float by AdjustParamProperty(0f, 0f, 100f, R.string.label_grain) //0 100


    var sharpenValue: Float by AdjustParamProperty(0f, 0f, 100f, R.string.label_sharpen) //0 100


    //endregion

    //region Blur values

    var blurType: BlurType by RequestRenderProperty(BlurType.NONE)

    var blurExcludeSize: Float by RequestRenderProperty(0.35f)

    var blurExcludePoint: PointF by RequestRenderProperty(PointF(0.5f, 0.5f))

    var blurExcludeBlurSize: Float by RequestRenderProperty(0.15f)

    var blurAngle: Float by RequestRenderProperty(PI.toFloat() / 2.0f)

    //endregion

    //region Curve values

    var curvesToolValue: CurvesToolValue = CurvesToolValue()

    //endregion

    private val textureView: TextureView

    private val layoutChangeListener =
        OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            updateTextureViewSize(right - left, bottom - top)
        }

    init {
        textureView = TextureView(context).apply {
            surfaceTextureListener = this@FilterableImageView
            isOpaque = true
        }
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
            updateTextureViewSize(width, height)
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

    val textureSizeChanged = EventHandler<SizeX>()

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
        get() = SizeX(textureView.width, textureView.height)


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
}

class AdjustParamProperty<T : Comparable<T>>(
    initialValue: T,
    minValue: T,
    maxValue: T,
    val labelResource: Int = 0
) : RangedProperty<FilterableImageView, T>(minValue, maxValue, RequestRenderProperty(initialValue))

open class RequestRenderProperty<T>(initialValue: T) :
    ReadWriteProperty<FilterableImageView, T> {
    private var value: T = initialValue

    override fun getValue(thisRef: FilterableImageView, property: KProperty<*>): T = value

    override fun setValue(thisRef: FilterableImageView, property: KProperty<*>, value: T) {
        if (value == this.value)
            return

        this.value = value
        thisRef.requestRender()
    }
}
