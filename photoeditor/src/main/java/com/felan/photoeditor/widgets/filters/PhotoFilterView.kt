package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.felan.photoeditor.R
import com.felan.photoeditor.widgets.PhotoEditor

class PhotoFilterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), PhotoEditor {

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_filter,
            this,
            false
        ) as ViewGroup
    }

    private val imagePlace by lazy {
        controlsContainer.findViewById<FrameLayout>(R.id.fl_image_place)
    }

    private val imageView by lazy {
        controlsContainer.findViewById<FilterableImageView>(R.id.image)
    }

    private val allControls = MutableList<FilterControlsView?>(3) { null }

    private val adjustControls by lazy {
        //        controlsContainer.findViewById<PhotoAdjustControlsView>(R.id.controls_adjust)
        val control = PhotoAdjustControlsView(context)
        control.bindWith(imageView)
        imagePlace.addView(
            control,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.height_controls),
                Gravity.BOTTOM
            )
        )
        allControls[0] = control
        control
    }
    //
    private val blurControls by lazy {
        //        controlsContainer.findViewById<BlurControlsView>(R.id.controls_blur)
        val control = BlurControlsView(context)
        control.bindWith(imageView)
        imagePlace.addView(control, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        allControls[1] = control
        control
    }
    //
    private val curveControls by lazy {
        //        controlsContainer.findViewById<CurveControlsView>(R.id.controls_curve)
        val control = CurveControlsView(context)
        control.bindWith(imageView)
        imagePlace.addView(control, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        allControls[2] = control
        control
    }

//    private val allControls by lazy {
//        arrayOf<FilterControlsView>(adjustControls, blurControls, curveControls)
//    }


    private val allButtons by lazy {
        arrayOf<View>(
            controlsContainer.findViewById(R.id.btn_adjust),
            controlsContainer.findViewById(R.id.btn_blur),
            controlsContainer.findViewById(R.id.btn_curve)
        )
    }

    init {
//        allControls.forEach { it.bindWith(imageView) }
        allButtons.forEachIndexed { i, it ->
            it.setOnClickListener { onControlSelectButtonClicked(i) }
        }

        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        onControlSelectButtonClicked(0)
    }


    private fun onControlSelectButtonClicked(index: Int) {
        allButtons.forEachIndexed { i, it -> it.isSelected = i == index }
        if (allControls[index] == null)
            allControls[index] = when (index) {
                0 -> adjustControls
                1 -> blurControls
                2 -> curveControls
                else -> null
            }
        allControls.forEachIndexed { i, it -> it?.visibility = if (i == index) VISIBLE else GONE }
    }

    override fun setImage(image: Bitmap) {
        imageView.image = image
    }

    override fun getResultImage(): Bitmap? =
        imageView.resultBitmap

}