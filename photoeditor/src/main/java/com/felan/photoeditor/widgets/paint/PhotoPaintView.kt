package com.felan.photoeditor.widgets.paint


import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.felan.photoeditor.R
import com.felan.photoeditor.widgets.PhotoEditor

class PhotoPaintView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), PhotoEditor {

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_paint,
            this,
            false
        ) as ViewGroup
    }

    private val imageViewPlace by lazy {
        controlsContainer.findViewById<FrameLayout>(R.id.fl_image_place)
    }

    private lateinit var paintRenderView: PhotoPaintRenderView

    private val controls by lazy {
        controlsContainer.findViewById<PaintControlsView>(R.id.controls_paint)
    }

    init {
        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun setImage(image: Bitmap) {
        imageViewPlace.removeAllViews()
        paintRenderView = PhotoPaintRenderView(context, image, 0)
        paintRenderView.init()
        imageViewPlace.addView(
            paintRenderView,
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        controls.bindWith(paintRenderView)
    }

    override fun getResultImage(): Bitmap =
        paintRenderView.resultBitmap

}