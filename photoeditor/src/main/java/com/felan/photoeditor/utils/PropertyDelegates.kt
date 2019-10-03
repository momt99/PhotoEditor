package com.felan.photoeditor.utils

import android.view.View
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ViewInvalidatorProperty<T>(
    initialValue: T,
    private val equalityChecker: (T, T) -> Boolean = { v1, v2 -> v1 == v2 },
    private val invalidator: (View) -> Unit = { v -> v.invalidate() }
) : ReadWriteProperty<View, T> {
    private var value: T = initialValue

    override fun getValue(thisRef: View, property: KProperty<*>): T = value

    override fun setValue(thisRef: View, property: KProperty<*>, value: T) {
        if (equalityChecker(this.value, value))
            return

        this.value = value
        invalidator(thisRef)
    }
}

open class RangedProperty<R, T : Comparable<T>>(
    val min: T, val max: T,
    val innerProperty: ReadWriteProperty<R, T>
) :
    ReadWriteProperty<R, T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T =
        innerProperty.getValue(thisRef, property)

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        when {
            value < min -> innerProperty.setValue(thisRef, property, min)
            value > max -> innerProperty.setValue(thisRef, property, max)
            else -> innerProperty.setValue(thisRef, property, value)
        }
    }

}