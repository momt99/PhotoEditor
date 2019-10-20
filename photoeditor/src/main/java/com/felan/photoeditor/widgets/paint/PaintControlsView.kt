package com.felan.photoeditor.widgets.paint

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import com.felan.photoeditor.R

class PaintControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var boundPaintView: PhotoPaintView? = null

    private val controlsContainer by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.design_paint_controls,
            this,
            false
        ) as ViewGroup
    }

    private val undoButton by lazy {
        controlsContainer.findViewById<ImageButton>(R.id.btn_undo)
    }

    private val colorPicker by lazy {
        controlsContainer.findViewById<MyColorPicker>(R.id.color_picker)
    }

    private val drawSwitchButton by lazy {
        controlsContainer.findViewById<ImageButton>(R.id.btn_switch_draw)
    }

    private val brushTypeButton by lazy {
        controlsContainer.findViewById<ImageButton>(R.id.btn_brush_type)
    }

    private val textSwitchButton by lazy {
        controlsContainer.findViewById<ImageButton>(R.id.btn_switch_text)
    }

    private val textTypeButton by lazy {
        controlsContainer.findViewById<ImageButton>(R.id.btn_text_type)
    }

    init {
        addView(controlsContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        undoButton.setOnClickListener { boundPaintView?.undo() }

        drawSwitchButton.setOnClickListener { switchToMode(PaintMode.DRAW) }
        textSwitchButton.setOnClickListener { switchToMode(PaintMode.TEXT) }

        brushTypeButton.setOnClickListener { brushTypeButtonClicked() }
        textTypeButton.setOnClickListener { textTypeButtonClicked() }

        updateButtonsSelections(PaintMode.DRAW)
    }

    fun bindWith(paintView: PhotoPaintView) {
        this.boundPaintView = paintView
        colorPicker.selectedColorChanged.clearHandlers()
        colorPicker.selectedColorChanged += { paintView.setCurrentColor(it) }
        colorPicker.selectedColorChanged(colorPicker.selectedColor)
        colorPicker.selectedWeightChanged.clearHandlers()
        colorPicker.selectedWeightChanged += { paintView.setCurrentBrushWeight(it) }
        colorPicker.selectedWeightChanged(colorPicker.selectedWeight)
        paintView.paintModeChanged += { currentPaintMode = it }
    }

    enum class PaintMode {
        DRAW, TEXT
    }

    var currentPaintMode: PaintMode = PaintMode.DRAW
        get
        private set(value) {
            if (field == value)
                return
            field = value
            updateButtonsSelections(value)
            boundPaintView?.run { colorPicker.setCurrentColor(currentColor) }
            println("Mode changed to:$value")
        }

    private fun updateButtonsSelections(mode: PaintMode) = when (mode) {
        PaintMode.DRAW -> {
            drawSwitchButton.isSelected = true
            textSwitchButton.isSelected = false
            brushTypeButton.visibility = View.VISIBLE
            textTypeButton.visibility = View.GONE
        }
        PaintMode.TEXT -> {
            textSwitchButton.isSelected = true
            drawSwitchButton.isSelected = false
            textTypeButton.visibility = View.VISIBLE
            brushTypeButton.visibility = View.GONE
        }
    }

    fun switchToMode(mode: PaintMode) = boundPaintView?.run {
        when (mode) {
            PaintMode.DRAW -> {
                switchToDraw()
            }
            PaintMode.TEXT -> {
                switchToText()
            }
        }
        currentPaintMode = mode
    }

    private fun brushTypeButtonClicked() =
        ListPopupWindow(context).apply {
            setAdapter(object : BaseAdapter() {
                val previewImages = intArrayOf(
                    R.drawable.paint_radial_preview,
                    R.drawable.paint_elliptical_preview,
                    R.drawable.paint_neon_preview
                )

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val view = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.item_brush_type, parent, false)

                    view.findViewById<ImageView>(R.id.image)
                        .setImageResource(previewImages[position])
                    view.findViewById<RadioButton>(R.id.radio).isChecked =
                        boundPaintView?.currentBrush == position
                    view.setOnClickListener {
                        boundPaintView?.setBrush(position)
                        dismiss()
                    }
                    return view
                }

                override fun getItem(position: Int): Any = previewImages[position]

                override fun getItemId(position: Int): Long = position.toLong()

                override fun getCount(): Int = previewImages.size
            })
            width = (resources.getDimensionPixelSize(R.dimen.width_brush_item))
            anchorView = brushTypeButton
            show()
        }


    private fun textTypeButtonClicked() = ListPopupWindow(context).apply {
        setAdapter(object : BaseAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_text_type, parent, false)

                val stroke = position == 0

                view.findViewById<FrameLayout>(R.id.text_place).apply {
                    removeAllViews()
                    isFocusable = true
                    addView(EditTextOutline(context).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        isEnabled = false
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        setStrokeWidth(resources.getDimension(R.dimen.width_stroke_text_type))
                        setTextColor(if (stroke) Color.WHITE else Color.BLACK)
                        setStrokeColor(if (stroke) Color.BLACK else Color.TRANSPARENT)
                        setPadding(0, 0, 0, 0)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        setTypeface(null, Typeface.BOLD)
                        setText(if (stroke) R.string.outlined else R.string.regular)
                    }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                }
                view.findViewById<RadioButton>(R.id.radio).isChecked =
                    boundPaintView?.selectedStroke == stroke
                view.setOnClickListener {
                    boundPaintView?.setStroke(stroke)
                    dismiss()
                }
                return view
            }

            override fun getItem(position: Int): Any? = null

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getCount(): Int = 2
        })
        width = (resources.getDimensionPixelSize(R.dimen.width_brush_item))
        anchorView = textTypeButton
        show()
    }

}