package ru.cardsmobile.customlayoutmanager

import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutParams
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class CustomLayoutManager : RecyclerView.LayoutManager() {

    var currentLayout: DefaultScene = StackScene(this)

    override fun generateDefaultLayoutParams() = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (width == 0) return

        if (itemCount == 0) {
            removeAllViews()
            return
        }

        if (childCount == 0 && state.isPreLayout) return

        detachAndScrapAttachedViews(recycler)
        fill(recycler)
    }

    private fun fill(recycler: RecyclerView.Recycler) {
        currentLayout.run {
            measureChildSize(this)
            visibleRange().forEach { position -> addMeasureAndLayoutChild(childCount, recycler, position) }
        }
    }

    private fun addMeasureAndLayoutChild(index: Int, recycler: RecyclerView.Recycler, position: Int,
                                         layout: DefaultScene = currentLayout): View {
        val view = recycler.getViewForPosition(position)
        val (x, y) = currentLayout.getCardViewCoords(position)
        addView(view, index)
        measureChild(view, 0, 0)
        val size = layout.cardSize
        layoutDecorated(view, x, y, x + size.x, y + size.y)
        return view
    }

    private fun measureChildSize(layout: DefaultScene) {
        layout.updateCardSize(width - paddingLeft - paddingRight)
    }

    override fun canScrollVertically() = true

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val consumed = currentLayout.scrollBy(dy)
        // requestLayout ignored
        onLayoutChildren(recycler, state)
        return consumed
    }
}

typealias Coords = Pair<Int, Int>

sealed class DefaultScene(val layoutManager: CustomLayoutManager) {
    val CARD_PROPORTION = 1.57f
    val MAX_VISIBLE_STACK_CARDS = 10

    val cardSize = Point(-1, -1)

    var scrollOffset: Int = 0

    open fun getCardViewCoords(position: Int) =
            Coords(layoutManager.paddingLeft,
                    scrollOffset + layoutManager.height - layoutManager.paddingBottom - (position + 1) * cardSize.y)

    fun updateCardSize(measuredChildWidth: Int) {
        cardSize.set(measuredChildWidth, (measuredChildWidth.toFloat() / CARD_PROPORTION).toInt())
    }

    open fun visibleRange(): IntProgression {
        val start = Math.min((scrollOffset + layoutManager.height) / cardSize.y + 1, layoutManager.itemCount - 1)
        val end = Math.max((scrollOffset) / cardSize.y - 1, 0)
        return IntProgression.fromClosedRange(start, end, -1)
    }

    fun scrollBy(dy: Int): Int {
        val consumed = if (dy < 0) {
            Math.min(-dy, getMaxScroll() - scrollOffset)
        } else {
            -Math.min(scrollOffset, dy)
        }
        scrollOffset += consumed
        return -consumed
    }

    fun getMaxScroll() = layoutManager.run { itemCount * cardSize.y - height + paddingTop + paddingBottom}
}

class LinearScene(layoutManager: CustomLayoutManager) : DefaultScene(layoutManager)

class StackScene(layoutManager: CustomLayoutManager) : DefaultScene(layoutManager) {

    private fun distortion(x: Double) = when {
        x > 0 -> 2.0 / (1.0 + Math.pow(0.6, 1.1 * x)) - 1.0
        else -> cardSize.y.toDouble() / layoutManager.run { height - paddingTop  - paddingBottom }.toDouble() * x / 1.02f
    }

    override fun getCardViewCoords(position: Int): Coords {
        val baseOffset = layoutManager.run { height - paddingTop - paddingBottom - cardSize.y }
        val childTop = baseOffset - cardSize.y * position + scrollOffset
        val x = scale((baseOffset - childTop).toFloat(), (MAX_VISIBLE_STACK_CARDS * cardSize.y).toFloat(), 21f)
        var stackTop = Math.round(baseOffset - baseOffset * distortion(x.toDouble())).toInt()
        if (layoutManager.itemCount - position - 3 < baseOffset / cardSize.y) {
            stackTop = Math.min(stackTop, cardSize.y * (layoutManager.itemCount - position - 1))
        }
        return Coords(layoutManager.paddingLeft, layoutManager.paddingTop + stackTop)
    }

    private fun scale(value: Float, sourceMax: Float, destMax: Float) = value * destMax / sourceMax

    override fun visibleRange(): IntProgression {
        val end = if (cardSize.y > 0) Math.max((scrollOffset - layoutManager.height) / cardSize.y - 1, 0) else MAX_VISIBLE_STACK_CARDS
        val start = Math.min(end + MAX_VISIBLE_STACK_CARDS, layoutManager.itemCount - 1)
        return IntProgression.fromClosedRange(start, end, -1)
    }
}