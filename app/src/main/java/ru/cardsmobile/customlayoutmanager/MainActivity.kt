package ru.cardsmobile.customlayoutmanager

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        with(recyclerView) {
            addItemDecoration(CardDecoration(resources.getDimensionPixelSize(R.dimen.margin)))
            layoutManager = CustomLayoutManager(this)
            adapter = CardsAdapter()
            itemAnimator = CustomItemAnimator()
        }

        fabLinear.setOnClickListener { (recyclerView.layoutManager as? CustomLayoutManager)?.showLinear() }
        fabStack.setOnClickListener { (recyclerView.layoutManager as? CustomLayoutManager)?.showStack() }
        fabGrid.setOnClickListener { (recyclerView.layoutManager as? CustomLayoutManager)?.showGrid() }
    }
}

class CardDecoration(val margin: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        outRect?.set(margin, margin / 2, margin, margin / 2)
    }
}

class CustomCardVewHolder(view: CustomCardView) : RecyclerView.ViewHolder(view)

const val ITEMS_COUNT = 100

class CardsAdapter : RecyclerView.Adapter<CustomCardVewHolder>() {

    private val cardColors = listOf(
            Color.parseColor("#E57373"),
            Color.parseColor("#F06292"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#9575CD"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#4FC3F7"),
            Color.parseColor("#4DD0E1"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#81C784"),
            Color.parseColor("#AED581"),
            Color.parseColor("#DCE775"),
            Color.parseColor("#FFF176"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#A1887F"),
            Color.parseColor("#E0E0E0"),
            Color.parseColor("#90A4AE")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            CustomCardVewHolder(CustomCardView(parent.context))

    override fun getItemCount() = ITEMS_COUNT

    override fun onBindViewHolder(holder: CustomCardVewHolder?, position: Int) {
        holder?.let {
            with(holder.itemView as CustomCardView) {
                translationX= 0f
                translationY= 0f
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
                cardBackgroundColor = ColorStateList.valueOf(cardColors[position % cardColors.size])
                textView.text = position.toString()
            }
        }
    }

}
