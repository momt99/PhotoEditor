package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.view.ViewCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.core.widget.NestedScrollView
import com.felan.photoeditor.R
import com.felan.photoeditor.utils.setOnEndListener
import com.felan.photoeditor.widgets.Bindable
import com.felan.photoeditor.widgets.MySeekBar
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible

class PhotoAdjustControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FilterControlsView(context, attrs, defStyleAttr) {

    private var boundFilterableImageView: FilterableImageView? = null

    private val mainContainer: LinearLayout

    init {
        mainContainer = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        val scrollingContainer = NestedScrollView(context).apply {
            addView(
                mainContainer,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
        }

        addView(
            scrollingContainer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    private val textShowAnimation = AnimationUtils.loadAnimation(context, R.anim.text_show)

    private val textHideAnimation = AnimationUtils.loadAnimation(context, R.anim.text_hide)

    override fun bindWith(img: FilterableImageView) {
        boundFilterableImageView = img

        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.height_adjust_item)
        )
        arrayOf(
            FilterableImageView::enhanceValue,
            FilterableImageView::exposureValue,
            FilterableImageView::contrastValue,
            FilterableImageView::warmthValue,
            FilterableImageView::saturationValue,
            FilterableImageView::fadeValue,
            FilterableImageView::vignetteValue,
            FilterableImageView::grainValue,
            FilterableImageView::sharpenValue
        ).forEach { prop ->
            mainContainer.addView(
                createSeekBarForProperty(prop, img),
                ViewGroup.LayoutParams(lp)
            )
        }

        mainContainer.addView(
            createSeekBarForProperty(FilterableImageView::shadowsValue, img, R.id.shadows_control),
            ViewGroup.LayoutParams(lp)
        )
        mainContainer.addView(
            createColorRadiosForProperty(
                intArrayOf(
                    0x00000000, -0xb2b3, -0xb7fde, -0x3300, -0x7e2d7f,
                    -0x8e3a2a, -0xff8d44, -0x99d26f
                ),
                FilterableImageView::tintShadowsColor, img,
                findViewById<View>(R.id.shadows_control).findViewById(R.id.seekbar)
            ), ViewGroup.LayoutParams(lp)
        )

        mainContainer.addView(
            createSeekBarForProperty(
                FilterableImageView::highlightsValue,
                img,
                R.id.highlights_control
            ),
            ViewGroup.LayoutParams(lp)
        )
        mainContainer.addView(
            createColorRadiosForProperty(
                intArrayOf(
                    0x00000000, -0x106d7a, -0x15315e, -0xd1e84, -0x5b1252,
                    -0x76231b, -0xd17438, -0x32671b
                ),
                FilterableImageView::tintHighlightsColor, img,
                findViewById<View>(R.id.highlights_control).findViewById(R.id.seekbar)
            ), ViewGroup.LayoutParams(lp)
        )
    }

    private fun createSeekBarForProperty(
        property: KMutableProperty1<FilterableImageView, Float>,
        img: FilterableImageView,
        layoutId: Int = 0
    ): View =
        LayoutInflater.from(context).inflate(R.layout.design_adjust_item, this, false).apply {
            if (layoutId != 0)
                id = layoutId
            property.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val delegate = property.getDelegate(img) as AdjustParamProperty<Float>

            val tvLabel = findViewById<TextView>(R.id.tv_label).apply {
                setText(delegate.labelResource)
            }
            val tvValue = findViewById<TextView>(R.id.tv_value).apply {

            }

            findViewById<MySeekBar>(R.id.seekbar).apply {
                min = delegate.min
                max = delegate.max
                if (min < 0)
                    enableMiddlePoint = true
                progressChanged += { args ->
                    property.set(img, args.progress)
                    tvValue.text = String.format("%+.0f", args.progress)
                }
                progress = property.get(img)
                pressedChanged += { pressed ->
                    if (pressed) {
                        tvLabel.startAnimation(textHideAnimation)
                        tvValue.visibility = View.VISIBLE
                        textHideAnimation.setOnEndListener { tvLabel.visibility = View.GONE }
                        tvValue.startAnimation(textShowAnimation)
                    } else {
                        tvLabel.visibility = View.VISIBLE
                        tvLabel.startAnimation(textShowAnimation)
                        tvValue.startAnimation(textHideAnimation)
                        textHideAnimation.setOnEndListener { tvValue.visibility = View.GONE }
                    }
                }
            }


        }

    private fun createColorRadiosForProperty(
        colors: IntArray,
        property: KMutableProperty1<FilterableImageView, Int>,
        img: FilterableImageView,
        relatedSeekBar: MySeekBar
    ): RadioGroup = RadioGroup(context).apply {
        orientation = RadioGroup.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        ViewCompat.setPaddingRelative(
            this,
            resources.getDimensionPixelSize(R.dimen.width_label) + resources.getDimensionPixelSize(R.dimen.margin_normal),
            0,
            resources.getDimensionPixelSize(R.dimen.margin_normal),
            0
        )
        colors.forEach { color ->
            this.addView(
                AppCompatRadioButton(context).apply {
                    id = color  //We use color as the id
                    CompoundButtonCompat.setButtonTintList(
                        this,
                        ColorStateList.valueOf(if (color == 0) Color.WHITE else color)
                    )
                },
                RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }
        setOnCheckedChangeListener { _, checkedId ->
            property.set(img, checkedId)
            relatedSeekBar.setAllColors(if (checkedId == 0) Color.WHITE else checkedId)
        }
        check(property.get(img))
    }
}