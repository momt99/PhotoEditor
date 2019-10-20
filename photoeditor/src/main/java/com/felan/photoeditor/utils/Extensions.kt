package com.felan.photoeditor.utils

import android.content.res.Resources
import android.view.animation.Animation
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

open class EmptyAnimationListener : Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation?) = Unit

    override fun onAnimationEnd(animation: Animation?) = Unit

    override fun onAnimationStart(animation: Animation?) = Unit
}

fun Animation.setOnEndListener(listener: (Animation?) -> Unit) =
    setAnimationListener(object : EmptyAnimationListener() {
        override fun onAnimationEnd(animation: Animation?) = listener(animation)
    })


@ColorInt
fun Resources.Theme?.getColorAttribute(@AttrRes attrId: Int, @ColorInt defColor: Int): Int {
    var retVal = defColor
    this?.obtainStyledAttributes(intArrayOf(attrId))
        ?.apply { retVal = getColor(0, defColor) }
        ?.also { it.recycle() }
    return retVal
}

infix fun Int.safeRangeTo(that: Int): IntRange =
    if (this <= that) IntRange(this, that) else IntRange(that, this)