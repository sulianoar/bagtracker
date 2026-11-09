package com.stansa.bagtracker.ui.flat

import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stansa.bagtracker.data.ItemWithTags
import com.stansa.bagtracker.databinding.ItemRowBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FlatListAdapter(
    private val onClick: (ItemWithTags) -> Unit
) : ListAdapter<ItemWithTags, FlatListAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iwt = getItem(position)
        val item = iwt.item
        val b = holder.binding

        b.textEmoji.text = item.emoji
        b.textName.text = item.name
        b.textQuantity.text = "${item.quantity} ${item.quantityUnit}"

        // Condition badge
        b.textCondition.text = item.condition
        b.textCondition.setTextColor(when (item.condition) {
            "neuf"          -> Color.parseColor("#00CEC9")
            "bon"           -> Color.parseColor("#55EFC4")
            "usé"           -> Color.parseColor("#FDCB6E")
            "à remplacer"   -> Color.parseColor("#E17055")
            "cassé"         -> Color.parseColor("#D63031")
            else            -> Color.WHITE
        })

        // Expiration badge
        val now = System.currentTimeMillis()
        if (item.expirationDate != null) {
            val diff = item.expirationDate - now
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            b.textExpiry.visibility = View.VISIBLE
            b.textExpiry.text = when {
                diff < 0          -> "⛔ Périmé"
                days == 0L        -> "⛔ Expire auj."
                days <= 2         -> "🔴 J-$days"
                days <= 7         -> "🟠 J-$days"
                days <= 30        -> "🟡 J-$days"
                else              -> "🟢 J-$days"
            }
            b.textExpiry.setTextColor(when {
                diff < 0          -> Color.parseColor("#D63031")
                days <= 2         -> Color.parseColor("#D63031")
                days <= 7         -> Color.parseColor("#E17055")
                days <= 30        -> Color.parseColor("#FDCB6E")
                else              -> Color.parseColor("#55EFC4")
            })
        } else {
            b.textExpiry.visibility = View.GONE
        }

        // Tags
        b.textTags.text = iwt.tags.joinToString(" ") { "#${it.name}" }
        b.textTags.visibility = if (iwt.tags.isEmpty()) View.GONE else View.VISIBLE

        // Prix
        if (item.priceCents != null) {
            b.textPrice.visibility = View.VISIBLE
            b.textPrice.text = "%.2f€".format(item.priceCents / 100.0)
        } else {
            b.textPrice.visibility = View.GONE
        }

        // Poids
        if (item.weightGrams != null) {
            b.textWeight.visibility = View.VISIBLE
            b.textWeight.text = if (item.weightGrams >= 1000)
                "${"%.1f".format(item.weightGrams / 1000.0)}kg"
            else "${item.weightGrams}g"
        } else {
            b.textWeight.visibility = View.GONE
        }

        b.root.setOnClickListener { onClick(iwt) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ItemWithTags>() {
            override fun areItemsTheSame(a: ItemWithTags, b: ItemWithTags) = a.item.id == b.item.id
            override fun areContentsTheSame(a: ItemWithTags, b: ItemWithTags) = a == b
        }
    }
}