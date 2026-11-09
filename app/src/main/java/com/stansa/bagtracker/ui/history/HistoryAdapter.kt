package com.stansa.bagtracker.ui.history

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stansa.bagtracker.data.MovementLog
import com.stansa.bagtracker.databinding.ItemHistoryRowBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<MovementLog, HistoryAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    inner class ViewHolder(val binding: ItemHistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = getItem(position)
        val b = holder.binding

        b.textDate.text = dateFormat.format(Date(log.timestamp))

        b.textAction.text = when (log.action) {
            "ADD"    -> "➕ Ajouté"
            "MOVE"   -> "🔀 Déplacé"
            "DELETE" -> "🗑️ Supprimé"
            else     -> log.action
        }

        b.textFrom.text = if (log.fromContainerName.isBlank()) "—" else "De : ${log.fromContainerName}"
        b.textTo.text   = if (log.toContainerName.isBlank())   "—" else "Vers : ${log.toContainerName}"

        b.textFrom.visibility = if (log.action == "ADD") View.GONE else View.VISIBLE
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MovementLog>() {
            override fun areItemsTheSame(a: MovementLog, b: MovementLog) = a.id == b.id
            override fun areContentsTheSame(a: MovementLog, b: MovementLog) = a == b
        }
    }
}