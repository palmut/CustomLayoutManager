package ru.cardsmobile.customlayoutmanager

import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutParams
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class CustomLayoutManager(val recyclerView: RecyclerView) : RecyclerView.LayoutManager() {

    var currentLayout: BaseScene = GridScene(this)
        set(value) {
            preLayout = field
            field = value.from(field)
            recyclerView.adapter.notifyItemRangeChanged(0, itemCount)
        }
    var preLayout: BaseScene? = null

    override fun generateDefaultLayoutParams() = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    override fun supportsPredictiveItemAnimations() = true

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (width == 0) return

        if (itemCount == 0) {
            removeAllViews()
            return
        }

        if (childCount == 0 && state.isPreLayout) return

        detachAndScrapAttachedViews(recycler)
        fill(recycler, state)
    }

    private fun fill(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val layout = if (state.isPreLayout) preLayout else currentLayout
        layout?.run {
            measureChildSize(this)
            visibleRange(state.itemCount).forEach { position -> addMeasureAndLayoutChild(childCount, recycler, position, this) }
        }
    }

    private fun addMeasureAndLayoutChild(index: Int, recycler: RecyclerView.Recycler, position: Int,
                                         layout: BaseScene = currentLayout): View {
        val view = recycler.getViewForPosition(position)
        val (x, y) = layout.getCardViewCoords(position)
        addView(view, index)
        measureChild(view, 0, 0)
        val size = layout.cardSize
        layoutDecorated(view, x, y, x + size.x, y + size.y)
        return view
    }

    private fun measureChildSize(layout: BaseScene) {
        layout.updateCardSize(width - paddingLeft - paddingRight)
    }

    override fun canScrollVertically() = true

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val consumed = currentLayout.scrollBy(dy)
        // requestLayout ignored
        onLayoutChildren(recycler, state)
        return consumed
    }

    fun showLinear() {
        currentLayout = LinearScene(this)
    }

    fun showStack() {
        currentLayout = StackScene(this)
    }

    fun showGrid() {
        currentLayout = GridScene(this)
    }
}

typealias Coords = Pair<Int, Int>

sealed class BaseScene(val layoutManager: CustomLayoutManager) {
    val CARD_PROPORTION = 1.57f
    val MAX_VISIBLE_STACK_CARDS = 10

    val cardSize = Point(-1, -1)

    var scrollOffset: Int = 0

    open fun getCardViewCoords(position: Int) =
            Coords(layoutManager.paddingLeft,
                    scrollOffset + layoutManager.height - layoutManager.paddingBottom - (position + 1) * cardSize.y)

    open fun updateCardSize(measuredChildWidth: Int) {
        cardSize.set(measuredChildWidth, (measuredChildWidth.toFloat() / CARD_PROPORTION).toInt())
    }

    open fun visibleRange(itemCount: Int): IntProgression {
        val start = Math.min((scrollOffset + layoutManager.height) / cardSize.y + 1, itemCount - 1)
        val end = Math.max((scrollOffset) / cardSize.y - 1, 0)
        return IntProgression.fromClosedRange(start, end, -1)
    }

    open fun scrollBy(dy: Int): Int {
        val consumed = if (dy < 0) {
            Math.min(-dy, getMaxScroll() - scrollOffset)
        } else {
            -Math.min(scrollOffset, dy)
        }
        scrollOffset += consumed
        return -consumed
    }

    open fun getMaxScroll() = layoutManager.run { itemCount * cardSize.y - height + paddingTop + paddingBottom }

    open fun from(layout: BaseScene): BaseScene {
        updateCardSize(layoutManager.run { width - paddingLeft - paddingRight })
        scrollOffset = layout.scrollOffset
        fixScrollOffset()
        return this
    }

    open fun fixScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()))
    }
}

class LinearScene(layoutManager: CustomLayoutManager) : BaseScene(layoutManager)

class StackScene(layoutManager: CustomLayoutManager) : BaseScene(layoutManager) {

    private fun distortion(x: Double) = when {
        x > 0 -> 2.0 / (1.0 + Math.pow(0.6, 1.1 * x)) - 1.0
        else -> cardSize.y.toDouble() / layoutManager.run { height - paddingTop - paddingBottom }.toDouble() * x / 1.02f
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

    override fun visibleRange(itemCount: Int): IntProgression {
        val end = if (cardSize.y > 0) Math.max((scrollOffset - layoutManager.height) / cardSize.y - 1, 0) else MAX_VISIBLE_STACK_CARDS
        val start = Math.min(end + MAX_VISIBLE_STACK_CARDS, itemCount - 1)
        return IntProgression.fromClosedRange(start, end, -1)
    }
}

open class GridScene(layoutManager: CustomLayoutManager) : BaseScene(layoutManager) {

    private var internalScrollOffset
        get() = scrollOffset / 4
        set(value) {
            scrollOffset = value * 4
        }

    override fun updateCardSize(measuredChildWidth: Int) {
        cardSize.set(measuredChildWidth / 2, (measuredChildWidth.toFloat() / CARD_PROPORTION / 2).toInt())
    }

    override fun getCardViewCoords(position: Int) =
            Pair(layoutManager.paddingLeft + if (position % 2 == 0) 0 else cardSize.x,
                    internalScrollOffset + layoutManager.height - layoutManager.paddingBottom - (position / 2 + 1) * cardSize.y)

    override fun visibleRange(itemCount: Int): IntProgression {
        val start = Math.min((layoutManager.height * 2 + internalScrollOffset) / cardSize.y * 2, itemCount - 1)
        val end = Math.max((internalScrollOffset - layoutManager.height) / cardSize.y * 2, 0)
        return IntProgression.fromClosedRange(start, end, -1)
    }

    override fun getMaxScroll() = layoutManager.run { (itemCount + 1) / 2 * cardSize.y - height + paddingBottom + paddingTop }

    override fun scrollBy(dy: Int): Int {
        val consumed = if (dy < 0) {
            Math.min(-dy, getMaxScroll() - internalScrollOffset)
        } else {
            -Math.min(internalScrollOffset, dy)
        }
        internalScrollOffset += consumed
        return -consumed
    }

    override fun fixScrollOffset() {
        internalScrollOffset = Math.max(0, Math.min(internalScrollOffset, getMaxScroll()))
    }

}
