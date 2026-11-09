package com.stansa.bagtracker.ui.inventory

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stansa.bagtracker.data.ItemWithTags
import com.stansa.bagtracker.databinding.ItemRowBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InventoryAdapter(
    private val onClick: (ItemWithTags) -> Unit,
    private val onLongClick: (ItemWithTags) -> Unit
) : ListAdapter<ItemWithTags, InventoryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iwt = getItem(position)
        val item = iwt.item
        val b = holder.binding

        b.textEmoji.text = item.emoji
        b.textName.text = item.name
        b.textQuantity.text = "${item.quantity} ${item.quantityUnit}"

        // Condition badge
        b.badgeCondition.text = item.condition
        b.badgeCondition.setBackgroundColor(conditionColor(item.condition))

        // Container badge
        b.badgeContainer.visibility = if (item.isContainer) android.view.View.VISIBLE else android.view.View.GONE

        // Expiry badge
        item.expirationDate?.let { exp ->
            val now = System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(exp - now)
            b.textExpiry.visibility = android.view.View.VISIBLE
            b.textExpiry.text = when {
                days < 0  -> "⛔ Périmé"
                days == 0L -> "⛔ Aujourd'hui"
                days == 1L -> "🔴 Demain"
                days <= 3  -> "🔴 ${days}j"
                days <= 7  -> "🟠 ${days}j"
                else       -> "🟢 ${SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(Date(exp))}"
            }
            b.textExpiry.setTextColor(when {
                days < 0  -> Color.parseColor("#FF4444")
                days <= 3  -> Color.parseColor("#FF4444")
                days <= 7  -> Color.parseColor("#FFA500")
                else       -> Color.parseColor("#44FF88")
            })
        } ?: run { b.textExpiry.visibility = android.view.View.GONE }

        // Tags chips
        b.chipGroupTags.removeAllViews()
        iwt.tags.take(3).forEach { tag ->
            val chip = com.google.android.material.chip.Chip(b.root.context).apply {
                text = tag.name
                textSize = 10f
                isClickable = false
                setChipBackgroundColorResource(android.R.color.transparent)
                try { setChipStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor(tag.color))) }
                catch (e: Exception) {}
            }
            b.chipGroupTags.addView(chip)
        }

        // Photo
        if (item.photoPath != null && File(item.photoPath).exists()) {
            Glide.with(b.root.context).load(File(item.photoPath)).centerCrop().into(b.imagePhoto)
            b.imagePhoto.visibility = android.view.View.VISIBLE
        } else {
            b.imagePhoto.visibility = android.view.View.GONE
        }

        b.root.setOnClickListener { onClick(iwt) }
        b.root.setOnLongClickListener { onLongClick(iwt); true }
    }

    private fun conditionColor(condition: String) = when (condition) {
        "neuf"         -> Color.parseColor("#44FF88")
        "bon"          -> Color.parseColor("#6C5CE8")
        "usé"          -> Color.parseColor("#FFA500")
        "à remplacer"  -> Color.parseColor("#FF4444")
        "cassé"        -> Color.parseColor("#880000")
        else           -> Color.parseColor("#888888")
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ItemWithTags>() {
            override fun areItemsTheSame(a: ItemWithTags, b: ItemWithTags) = a.item.id == b.item.id
            override fun areContentsTheSame(a: ItemWithTags, b: ItemWithTags) = a == b
        }
    }
}