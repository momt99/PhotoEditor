package com.felan.photoeditor.utils

import android.view.animation.Animation

open class EmptyAnimationListener : Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation?) = Unit

    override fun onAnimationEnd(animation: Animation?) = Unit

    override fun onAnimationStart(animation: Animation?) = Unit
}

fun Animation.setOnEndListener(listener: (Animation?) -> Unit) =
    setAnimationListener(object : EmptyAnimationListener() {
        override fun onAnimationEnd(animation: Animation?) = listener(animation)
    })