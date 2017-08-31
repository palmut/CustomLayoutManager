package ru.cardsmobile.customlayoutmanager

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import org.jetbrains.anko.margin
import org.jetbrains.anko.textSizeDimen

const val CARD_PROPORTION = 1.57f
const val CARD_SCALE = 0.95f

class CustomCardView : CardView {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle)

    val textView = TextView(context).apply { textSizeDimen = R.dimen.card_text_size }
    private val paddingScale = (1.0f - CARD_SCALE) / 2.0f

    init {
        radius = context.resources.getDimension(R.dimen.corner_radius)
        layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            margin = context.resources.getDimensionPixelSize(R.dimen.margin)
        }
        addView(textView,
                FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            // keep proportions if possible
            setMeasuredDimension(measuredWidth, (measuredWidth.toFloat() / CARD_PROPORTION).toInt())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // mandatory padding
        val horizontalPadding = paddingScale * w.toFloat()
        val verticalPadding = horizontalPadding / CARD_PROPORTION
        setPadding(horizontalPadding.toInt(), verticalPadding.toInt(), horizontalPadding.toInt(), verticalPadding.toInt())
    }
}