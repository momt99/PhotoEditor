package com.felan.photoeditor.widgets.filters

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.felan.photoeditor.widgets.Bindable

abstract class FilterControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Bindable<FilterableImageView>