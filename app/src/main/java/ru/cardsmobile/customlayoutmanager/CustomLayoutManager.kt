package ru.cardsmobile.customlayoutmanager

import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutParams
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class CustomLayoutManager : RecyclerView.LayoutManager() {

    var currentLayout: DefaultScene = LinearScene(this)

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

sealed class DefaultScene(private val layoutManager: CustomLayoutManager) {
    val CARD_PROPORTION = 1.57f

    val cardSize = Point(-1, -1)

    var scrollOffset: Int = 0

    fun getCardViewCoords(position: Int) =
            Coords(layoutManager.paddingLeft,
                    scrollOffset + layoutManager.height - layoutManager.paddingBottom - (position + 1) * cardSize.y)

    fun updateCardSize(measuredChildWidth: Int) {
        cardSize.set(measuredChildWidth, (measuredChildWidth.toFloat() / CARD_PROPORTION).toInt())
    }

    fun visibleRange(): IntProgression {
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