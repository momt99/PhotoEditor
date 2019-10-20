package com.felan.photoeditor.utils

import android.view.View
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> viewInvalidator(
    initialValue: T,
    equalityChecker: (T, T) -> Boolean = { v1, v2 -> v1 == v2 },
    disableFirstTimeEqualityCheck: Boolean = true,
    preInvalidate: (View, T) -> Unit = { _, _ -> Unit },
    postInvalidate: (View, T) -> Unit = { _, _ -> Unit }
) = ViewInvalidatorProperty(
    SimpleNormalProperty(initialValue),
    equalityChecker,
    disableFirstTimeEqualityCheck
) { view, value ->
    preInvalidate(view, value)
    view.invalidate()
    postInvalidate(view, value)
}

fun <T : Any> viewInvalidatorLateInit(
    equalityChecker: (T, T) -> Boolean = { v1, v2 -> v1 == v2 },
    preInvalidate: (View, T) -> Unit = { _, _ -> Unit },
    postInvalidate: (View, T) -> Unit = { _, _ -> Unit }
) = ViewInvalidatorProperty(SimpleLateInitProperty(), equalityChecker, true) { view, value ->
    preInvalidate(view, value)
    view.invalidate()
    postInvalidate(view, value)
}

class ViewInvalidatorProperty<T>(
    val innerProperty: ReadWriteProperty<View, T>,
    private val equalityChecker: (T, T) -> Boolean = { v1, v2 -> v1 == v2 },
    var disableFirstTimeEqualityCheck: Boolean = true,
    private val invalidator: (View, T) -> Unit = { v, _ -> v.invalidate() }
) : ReadWriteProperty<View, T> {

    constructor(
        initialValue: T,
        equalityChecker: (T, T) -> Boolean = { v1, v2 -> v1 == v2 },
        disableFirstTimeEqualityCheck: Boolean = true,
        invalidator: (View, T) -> Unit = { v, _ -> v.invalidate() }
    ) : this(
        SimpleNormalProperty(initialValue),
        equalityChecker,
        disableFirstTimeEqualityCheck,
        invalidator
    )

    override fun getValue(thisRef: View, property: KProperty<*>): T =
        innerProperty.getValue(thisRef, property)

    override fun setValue(thisRef: View, property: KProperty<*>, value: T) {
        if (!disableFirstTimeEqualityCheck) {
            if (equalityChecker(getValue(thisRef, property), value))
                return
        } else
            disableFirstTimeEqualityCheck = false

        innerProperty.setValue(thisRef, property, value)
        invalidator(thisRef, value)
    }
}

open class RangedProperty<in R, T : Comparable<T>>(
    val min: T, val max: T,
    val innerProperty: ReadWriteProperty<R, T>
) : ReadWriteProperty<R, T> {
    constructor(min: T, max: T, initialValue: T) : this(
        min,
        max,
        SimpleNormalProperty(initialValue)
    )

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

open class SimpleNormalProperty<R, T>(initialValue: T) : ReadWriteProperty<R, T> {
    protected var value: T = initialValue

    override fun getValue(thisRef: R, property: KProperty<*>): T = value

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.value = value
    }
}

open class SimpleLateInitProperty<in R, T : Any>() : ReadWriteProperty<R, T> {
    protected lateinit var value: T

    override fun getValue(thisRef: R, property: KProperty<*>): T = value

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.value = value
    }
}