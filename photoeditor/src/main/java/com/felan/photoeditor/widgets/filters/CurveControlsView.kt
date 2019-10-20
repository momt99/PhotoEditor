package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.graphics.Color
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
import com.felan.photoeditor.widgets.Bindable
import com.felan.photoeditor.widgets.CustomRadioGroupCheckedListener

typealias CurveType = CurvesToolValue.CurveType

class CurveControlsView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FilterControlsView(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    companion object {
        private val CURVE_TYPE_TAG_ID = R.id.tag_curve_type
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val typeSelectionLayout by lazy {
        Utilities.makeRadioButtonsForLabels(
            context, arrayOf("All", "Red", "Green", "Blue"),
            intArrayOf(Color.WHITE, Color.RED, Color.GREEN, Color.BLUE)
        )
    }

    private val curveViewContainer: FrameLayout
    private lateinit var curveView: CurveView

    private val allRadioButtons: Array<RadioButton>

    private var boundFilterableImageView: FilterableImageView? = null

    init {
        curveViewContainer = FrameLayout(context)
        addView(curveViewContainer,
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

        allRadioButtons = radioPairs.map { it.first }.toTypedArray()

        val checkedListener =
            CustomRadioGroupCheckedListener(radioPairs, this)

        radioPairs.asSequence()
            .map { it.first }
            .forEachIndexed { index, rbtn ->
                rbtn.setTag(CURVE_TYPE_TAG_ID, CurveType.values()[index])
                rbtn.setOnCheckedChangeListener(checkedListener)
            }
    }

    var curveType: CurveType =
        CurveType.LUMINANCE
        get
        set(value) {
            field = value
            allRadioButtons[value.value].isChecked = true
            curveView.setActiveType(value)
        }

    override fun bindWith(img: FilterableImageView) =
        img.run {
            curveView =
                CurveView(context, img.curvesToolValue)
            curveView.setDelegate { img.requestRender() }
            curveViewContainer.addView(
                curveView,
                LayoutParams(textureViewSize.width, textureViewSize.height, Gravity.CENTER)
            )
            boundFilterableImageView = this
            textureSizeChanged += { newSize ->
                curveView.updateLayoutParams {
                    width = newSize.width
                    height = newSize.height
                }
                curveView.setActualArea(0f, 0f, newSize.width.toFloat(), newSize.height.toFloat())
            }
        }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == null)
            return
        curveType = buttonView.getTag(CURVE_TYPE_TAG_ID) as CurvesToolValue.CurveType
    }

}