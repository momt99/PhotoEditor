package com.felan.photoeditor.widgets.crop

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.getColorAttribute
import com.felan.photoeditor.widgets.RotationWheel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs

class CropControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var boundCropView: CropView? = null

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_crop_controls,
            this,
            false
        ) as ViewGroup
    }

    private val rotationWheel by lazy {
        controlsContainer.findViewById<RotationWheel>(R.id.wheel_rotate)
    }

    private val rotateButton by lazy {
        controlsContainer.findViewById<View>(R.id.btn_rotate)
    }

    private val rotationTextView by lazy {
        controlsContainer.findViewById<TextView>(R.id.tv_rotation)
    }

    private val aspectRatioButton by lazy {
        controlsContainer.findViewById<AppCompatImageButton>(R.id.btn_aspect_ratio)
    }

    private val resetButton by lazy {
        controlsContainer.findViewById<View>(R.id.btn_reset)
    }

    init {
        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        rotateButton.setOnClickListener { onRotateButtonClicked() }

        aspectRatioButton.setOnClickListener { onAspectRationButtonClicked() }

        resetButton.setOnClickListener { onResetButtonClicked() }

        initRotationWheel()
    }

    private fun initRotationWheel() {
        with(rotationWheel)
        {
            rotationStarted += { boundCropView?.onRotationBegan() }
            rotationEnded += { boundCropView?.onRotationEnded() }
            valueChanged += {
                rotationTextView.text = "%.1f °".format(it)
                boundCropView?.setImageRotation(it)
            }
        }
    }


    private fun onRotateButtonClicked() {
        rotationWheel.value = 0f
        boundCropView?.rotate90Degrees()
    }

    private val aspectRatios by lazy {
        resources.getStringArray(R.array.aspect_ratios)
            .map { it.split(':') }
            .map { Pair(it.first().toFloat(), it.last().toFloat()) }
            .map {
                if (it.first == -1f) boundCropView?.originalRatio ?: 0f else it.first / it.second
            }
    }

    private var lastAspectRatioIndex = 0


    private fun updateAspectRationButtonColor(isOff: Boolean) {
        aspectRatioButton.isSelected = !isOff
    }

    private fun onAspectRationButtonClicked() {
        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(
                R.array.aspect_ratios_labels,
                lastAspectRatioIndex
            ) { dialog, which ->
                boundCropView?.setLockedAspectRatio(aspectRatios[which])
                lastAspectRatioIndex = which
                updateAspectRationButtonColor(which == 0)
                dialog.dismiss()
            }
            .show()
    }

    private fun onResetButtonClicked() {
        rotationWheel.value = 0f
        updateAspectRationButtonColor(true)
        boundCropView?.reset()
    }

    fun bindWith(cropView: CropView?) {
        this.boundCropView = cropView
    }


    var enableBottomPaddingBinding: Boolean = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boundCropView?.setBottomPadding(h.toFloat())
    }
}