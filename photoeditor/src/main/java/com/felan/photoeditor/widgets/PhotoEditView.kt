package com.felan.photoeditor.widgets

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.felan.photoeditor.R
import com.felan.photoeditor.widgets.crop.PhotoCropView
import com.felan.photoeditor.widgets.filters.PhotoFilterView
import com.felan.photoeditor.widgets.paint.PhotoPaintView


class PhotoEditView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_edit,
            this,
            false
        ) as FrameLayout
    }

    private val imageView by lazy {
        controlsContainer.findViewById<ImageView>(R.id.image)
    }

    private val modeButtonsContainer by lazy {
        controlsContainer.findViewById<View>(R.id.ll_mode_buttons_container)
    }


    private val doneCancelContainer by lazy {
        controlsContainer.findViewById<FrameLayout>(R.id.fl_done_cancel_container)
    }

    private val allButtons by lazy {
        arrayOf<View>(
            controlsContainer.findViewById(R.id.btn_filter),
            controlsContainer.findViewById(R.id.btn_paint),
            controlsContainer.findViewById(R.id.btn_crop)
        )
    }

    init {
        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        allButtons.forEach { view ->
            view.setOnClickListener { onModeSelectButtonClicked(view.id) }
        }

        controlsContainer.findViewById<View>(R.id.btn_done)
            .setOnClickListener { onDoneButtonClicked() }

        controlsContainer.findViewById<View>(R.id.btn_cancel)
            .setOnClickListener { onCancelButtonClicked() }
    }

    var image: Bitmap? = null
        get
        set(value) {
            if (field == value) return
            field = value
            imageView.setImageBitmap(value)
        }

    private var currentEditor: PhotoEditor? = null

    private fun onModeSelectButtonClicked(btnId: Int) {
        val editor: PhotoEditor = when (btnId) {
            R.id.btn_filter -> PhotoFilterView(context)
            R.id.btn_paint -> PhotoPaintView(context)
            R.id.btn_crop -> PhotoCropView(context)
            else -> return
        }

        editor.setImage(image ?: return)
        currentEditor = editor
        switchToEditMode(editor as View)
    }

    private fun onDoneButtonClicked() = currentEditor?.let {
        image = it.getResultImage()
        switchToNormalMode()
    }


    private fun onCancelButtonClicked() {
        switchToNormalMode()
    }

    private fun switchToEditMode(editor: View) {
        imageView.visibility = View.GONE
        modeButtonsContainer.visibility = View.GONE
        controlsContainer.addView(
            editor,
            0,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        doneCancelContainer.visibility = View.VISIBLE
    }

    private fun switchToNormalMode() {
        controlsContainer.removeView(currentEditor as View)
        doneCancelContainer.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        modeButtonsContainer.visibility = View.VISIBLE
    }
}

