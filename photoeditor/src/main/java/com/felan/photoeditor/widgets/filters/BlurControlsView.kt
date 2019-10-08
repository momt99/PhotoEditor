package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.Utilities
import com.felan.photoeditor.widgets.CustomRadioGroupCheckedListener

class BlurControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    companion object {
        private val BLUR_TYPE_TAG_ID = R.id.tag_blur_type
    }

    private val typeSelectionLayout by lazy {
        Utilities.makeRadioButtonsForLabels(context, "Off", "Radial", "Linear")
    }

    private val positionView: BlurPositionView

    private val positionViewContainer: FrameLayout

    private val allRadioButtons: Array<RadioButton>

    private var boundFilterableImageView: FilterableImageView? = null

    init {
        positionView = BlurPositionView(context)
        positionViewContainer = FrameLayout(context).apply {
            addView(
                positionView,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            )
        }
        addView(
            positionViewContainer,
            MarginLayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.height_controls) }
        )

        addView(
            typeSelectionLayout,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.height_controls),
                Gravity.BOTTOM
            )
        )

        val radioPairs = typeSelectionLayout.children
            .map {
                (it as ViewGroup).run {
                    Pair(getChildAt(0) as RadioButton, getChildAt(1) as TextView)
                }
            }.toList()

        val checkedListener =
            CustomRadioGroupCheckedListener(radioPairs, this)

        allRadioButtons = radioPairs.map { it.first }.toTypedArray()

        allRadioButtons
            .forEachIndexed { index, rbtn ->
                rbtn.setTag(BLUR_TYPE_TAG_ID, BlurType.values()[index])
                rbtn.setOnCheckedChangeListener(checkedListener)
            }

        positionView.valueChanged += { args -> onPositionViewUpdated(args) }
    }

    var blurType: BlurType =
        BlurType.NONE
        get
        set(value) {
            field = value
            allRadioButtons[value.value].isChecked = true
            positionView.setType(value)
            boundFilterableImageView?.blurType = value
        }

    fun bindWith(img: FilterableImageView) =
        img.run {
            boundFilterableImageView = this
            textureSizeChanged += { newSize ->
                positionView.updateLayoutParams {
                    width = newSize.width
                    height = newSize.height
                }
                positionView.setActualAreaSize(newSize.width.toFloat(), newSize.height.toFloat())
            }
        }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == null)
            return
        blurType = buttonView.getTag(BLUR_TYPE_TAG_ID) as BlurType
    }

    private fun onPositionViewUpdated(args: BlurPositionView.ValueChangedEventArgs) =
        boundFilterableImageView?.updateGrouped {
            blurExcludeSize = args.size
            blurExcludePoint = args.centerPoint
            blurExcludeBlurSize = args.falloff
            blurAngle = args.angle
        }
}