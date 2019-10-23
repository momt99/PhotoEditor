package com.felan.photoeditor.widgets

import android.graphics.Bitmap

interface PhotoEditor {
    fun setImage(image: Bitmap)
    fun getResultImage(): Bitmap?
}