package com.felan.photoeditor.widgets.crop

import com.felan.photoeditor.widgets.filters.*

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.felan.photoeditor.R

class PhotoCropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_crop,
            this,
            false
        ) as ViewGroup
    }

    private val imageView by lazy {
        controlsContainer.findViewById<CropView>(R.id.image)
    }

    private val controls by lazy {
        controlsContainer.findViewById<CropControlsView>(R.id.controls_crop)
    }

    init {
        controls.bindWith(imageView)

        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    fun setImage(bitmap: Bitmap) {
        imageView.setBitmap(bitmap, 0, true, false)
    }
}