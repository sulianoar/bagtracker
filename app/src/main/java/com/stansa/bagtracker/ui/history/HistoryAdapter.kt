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
        "ADD" -> "➕ ${log.itemName} — Ajouté"
        "MOVE" -> "🔀 ${log.itemName} — Déplacé"
        "DELETE" -> "🗑️ ${log.itemName} — Supprimé"
        else -> "${log.itemName} — ${log.action}"
    }

    b.textFrom.text =
        if (log.fromContainerName.isBlank()) "De : Racine"
        else "De : ${log.fromContainerName}"

    b.textTo.text =
        if (log.toContainerName.isBlank()) "Vers : Racine"
        else "Vers : ${log.toContainerName}"

    b.textFrom.visibility =
        if (log.action == "ADD") View.GONE else View.VISIBLE

    b.textTo.visibility =
        if (log.action == "DELETE") View.GONE else View.VISIBLE
}


    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MovementLog>() {
            override fun areItemsTheSame(a: MovementLog, b: MovementLog) = a.id == b.id
            override fun areContentsTheSame(a: MovementLog, b: MovementLog) = a == b
        }
    }
}
