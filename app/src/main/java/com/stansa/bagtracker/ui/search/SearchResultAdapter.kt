package com.stansa.bagtracker.ui.search

import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stansa.bagtracker.data.ItemWithTags
import com.stansa.bagtracker.databinding.ItemRowBinding
import java.util.concurrent.TimeUnit

class SearchResultAdapter(
    private val onClick: (ItemWithTags) -> Unit
) : ListAdapter<ItemWithTags, SearchResultAdapter.ViewHolder>(DIFF) {

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

        b.textCondition.text = item.condition
        b.textCondition.setTextColor(when (item.condition) {
            "neuf"        -> Color.parseColor("#00CEC9")
            "bon"         -> Color.parseColor("#55EFC4")
            "usé"         -> Color.parseColor("#FDCB6E")
            "à remplacer" -> Color.parseColor("#E17055")
            "cassé"       -> Color.parseColor("#D63031")
            else          -> Color.WHITE
        })

        val now = System.currentTimeMillis()
        if (item.expirationDate != null) {
            val diff = item.expirationDate - now
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            b.textExpiry.visibility = View.VISIBLE
            b.textExpiry.text = when {
                diff < 0   -> "⛔ Périmé"
                days == 0L -> "⛔ Expire auj."
                days <= 2  -> "🔴 J-$days"
                days <= 7  -> "🟠 J-$days"
                days <= 30 -> "🟡 J-$days"
                else       -> "🟢 J-$days"
            }
        } else {
            b.textExpiry.visibility = View.GONE
        }

        b.textTags.text = iwt.tags.joinToString(" ") { "#${it.name}" }
        b.textTags.visibility = if (iwt.tags.isEmpty()) View.GONE else View.VISIBLE

        b.textPrice.visibility = if (item.priceCents != null) View.VISIBLE else View.GONE
        item.priceCents?.let { b.textPrice.text = "%.2f€".format(it / 100.0) }

        b.textWeight.visibility = if (item.weightGrams != null) View.VISIBLE else View.GONE
        item.weightGrams?.let {
            b.textWeight.text = if (it >= 1000) "${"%.1f".format(it / 1000.0)}kg" else "${it}g"
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