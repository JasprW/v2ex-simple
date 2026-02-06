package im.fdx.v2ex.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.MyTab

class ChipTabAdapter(
    private val tabs: List<MyTab>,
    private var selectedIndex: Int,
    private val onTabClick: (Int) -> Unit
) : RecyclerView.Adapter<ChipTabAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_tab_chip, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chip = holder.chip
        chip.text = tabs[position].title
        applyChipColors(chip, position == selectedIndex)
        applyChipShape(chip, position == selectedIndex)
        chip.setOnClickListener { onTabClick(position) }
    }

    override fun getItemCount(): Int = tabs.size

    fun updateSelected(newIndex: Int) {
        if (newIndex == selectedIndex) return
        val oldIndex = selectedIndex
        selectedIndex = newIndex
        notifyItemChanged(oldIndex)
        notifyItemChanged(newIndex)
    }

    private fun applyChipColors(chip: Chip, isSelected: Boolean) {
        val backgroundColor = MaterialColors.getColor(
            chip,
            if (isSelected) R.attr.colorSecondaryContainer else R.attr.colorSurface
        )
        val textColor = MaterialColors.getColor(
            chip,
            if (isSelected) R.attr.colorOnSecondaryContainer else R.attr.colorOnSurface
        )
        val strokeColor = MaterialColors.getColor(chip, R.attr.colorOutline)
        val strokeWidth = 0f

        chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
        chip.setTextColor(textColor)
        chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        chip.chipStrokeWidth = strokeWidth
    }

    private fun applyChipShape(chip: Chip, isSelected: Boolean) {
        val density = chip.resources.displayMetrics.density
        val targetCornerPx = if (isSelected) 8f * density else 16f * density
        val currentCornerPx = chip.getTag(R.id.tag_chip_corner) as? Float

        if (currentCornerPx == null || !chip.isLaidOut) {
            chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(targetCornerPx)
                .build()
            chip.setTag(R.id.tag_chip_corner, targetCornerPx)
            return
        }

        if (currentCornerPx == targetCornerPx) return

        val animator = ValueAnimator.ofFloat(currentCornerPx, targetCornerPx)
        animator.duration = 150L
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(value)
                .build()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                chip.setTag(R.id.tag_chip_corner, targetCornerPx)
            }
        })
        animator.start()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chip: Chip = itemView.findViewById(R.id.chip_tab)
    }
}
