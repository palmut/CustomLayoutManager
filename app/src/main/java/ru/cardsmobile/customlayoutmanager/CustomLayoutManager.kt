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
        val size = layout.getCardSizeForPosition(position)
        layoutDecorated(view, x, y, x + size.x, y + size.y)
        return view
    }

    private fun measureChildSize(layout: DefaultScene) {
        layout.updateCardSize(width - paddingLeft - paddingRight)
    }
}

// просто для красоты
typealias Coords = Pair<Int, Int>

sealed class DefaultScene(private val layoutManager: CustomLayoutManager) {
    val CARD_PROPORTION = 1.57f

    val cardSize = Point(-1, -1)

    open fun getCardViewCoords(position: Int) =
            Coords(layoutManager.paddingLeft,
                    layoutManager.height - layoutManager.paddingBottom - (position + 1) * cardSize.y)

    open fun updateCardSize(measuredChildWidth: Int) {
        cardSize.set(measuredChildWidth, (measuredChildWidth.toFloat() / CARD_PROPORTION).toInt())
    }

    open fun visibleRange(): IntProgression {
        val start = Math.min((layoutManager.height) / cardSize.y + 1, layoutManager.itemCount - 1)
        val end = 0
        return IntProgression.fromClosedRange(start, end, -1)
    }

    open fun getCardSizeForPosition(position: Int) = cardSize
}

class LinearScene(layoutManager: CustomLayoutManager) : DefaultScene(layoutManager)