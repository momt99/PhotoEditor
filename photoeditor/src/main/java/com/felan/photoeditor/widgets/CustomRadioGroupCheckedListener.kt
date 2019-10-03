package com.felan.photoeditor.widgets

import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.TextView
import com.felan.photoeditor.R

class CustomRadioGroupCheckedListener(
    private val radios: List<Pair<RadioButton, TextView>>,
    private val additionalListener: CompoundButton.OnCheckedChangeListener? = null
) :
    CompoundButton.OnCheckedChangeListener {

    init {
        radios.forEach {
            it.first.setTag(R.id.tag_textview, it.second)
            it.second.setOnClickListener { _ -> it.first.performClick() }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == null)
            return

        (buttonView.getTag(R.id.tag_textview) as TextView).isSelected = isChecked

        if (!isChecked)
            return

        radios
            .asSequence()
            .map { pair -> pair.first }
            .filter { buttonView != it }
            .filter { it.isChecked }
            .forEach { it.isChecked = false }

        additionalListener?.onCheckedChanged(buttonView, isChecked)
    }
}