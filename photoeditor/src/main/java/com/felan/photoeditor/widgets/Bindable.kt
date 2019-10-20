package com.felan.photoeditor.widgets

interface Bindable<in T> {
    fun bindWith(obj: T)
}