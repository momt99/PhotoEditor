package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.felan.photoeditor.R

class PhotoFilterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_filter,
            this,
            false
        ) as ViewGroup
    }

    private val imageView by lazy {
        controlsContainer.findViewById<FilterableImageView>(R.id.image)
    }

    private val adjustControls by lazy {
        controlsContainer.findViewById<PhotoAdjustControlsView>(R.id.controls_adjust)
    }

    private val blurControls by lazy {
        controlsContainer.findViewById<BlurControlsView>(R.id.controls_blur)
    }

    private val curveControls by lazy {
        controlsContainer.findViewById<CurveControlsView>(R.id.controls_curve)
    }

    private val allControls by lazy {
        arrayOf<FilterControlsView>(adjustControls, blurControls, curveControls)
    }

    private val allButtons by lazy {
        arrayOf<View>(
            controlsContainer.findViewById(R.id.btn_adjust),
            controlsContainer.findViewById(R.id.btn_blur),
            controlsContainer.findViewById(R.id.btn_curve)
        )
    }

    init {
        allControls.forEach { it.bindWith(imageView) }
        allButtons.forEachIndexed { i, it ->
            it.setOnClickListener { onControlSelectButtonClicked(i) }
        }

        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        onControlSelectButtonClicked(0)
    }


    private fun onControlSelectButtonClicked(index: Int) {
        allButtons.forEachIndexed { i, it -> it.isSelected = i == index }
        allControls.forEachIndexed { i, it -> it.visibility = if (i == index) VISIBLE else GONE }
    }

    fun setImage(bitmap: Bitmap) {
        imageView.image = bitmap
    }
}