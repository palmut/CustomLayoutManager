package ru.cardsmobile.customlayoutmanager

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import org.jetbrains.anko.textSizeDimen

const val CARD_PROPORTION = 1.57f

class CustomCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : CardView(context, attrs, defStyle) {

    val textView = TextView(context).apply { textSizeDimen = R.dimen.card_text_size }

    init {
        radius = context.resources.getDimension(R.dimen.corner_radius)
        addView(textView,
                FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            setMeasuredDimension(measuredWidth, (measuredWidth.toFloat() / CARD_PROPORTION).toInt())
        }
    }
}