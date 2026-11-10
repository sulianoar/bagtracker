package com.stansa.bagtracker.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.stansa.bagtracker.R
import com.stansa.bagtracker.data.MovementLog
import com.stansa.bagtracker.databinding.FragmentItemDetailBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: ItemViewModel by activityViewModels()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val itemId = arguments?.getLong("itemId") ?: return

        lifecycleScope.launch {
            val iwt = vm.getItemWithTagsById(itemId) ?: return@launch
            val item = iwt.item

            binding.textName.text = "${item.emoji} ${item.name}"
            binding.textCondition.text = "État : ${item.condition}"
            binding.textQuantity.text = "Quantité : ${item.quantity} ${item.quantityUnit}"
            binding.textNotes.text = item.notes.ifEmpty { "Aucune note" }

            item.weightGrams?.let { binding.textWeight.text = "Poids : ${if (it >= 1000) "%.1f kg".format(it / 1000.0) else "$it g"}" }
            item.volumeCm3?.let { binding.textVolume.text = "Volume : $it cm³" }
            item.priceCents?.let { binding.textPrice.text = "Valeur : %.2f €".format(it / 100.0) }
            item.expirationDate?.let { binding.textExpiry.text = "Péremption : ${SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(it))}" }
            item.barcode?.let { binding.textBarcode.text = "Code-barre : $it" }

            // Photo
            item.photoPath?.let { path ->
                if (File(path).exists()) {
                    Glide.with(this@ItemDetailFragment).load(File(path)).into(binding.imagePhoto)
                    binding.imagePhoto.visibility = View.VISIBLE
                }
            }

            // Tags
            iwt.tags.forEach { tag ->
                binding.chipGroupTags.addView(Chip(requireContext()).apply { text = tag.name })
            }

            // GPS
            if (item.locationLat != null && item.locationLng != null) {
                binding.buttonOpenMap.visibility = View.VISIBLE
                binding.textLocation.text = item.locationName ?: "${item.locationLat}, ${item.locationLng}"
                binding.buttonOpenMap.setOnClickListener {
                    val geoUri = Uri.parse("geo:${item.locationLat},${item.locationLng}?q=${item.locationLat},${item.locationLng}")
                    startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                }
            }

            // Chemin
            val path = vm.getFullPath(itemId)
            binding.textPath.text = path.joinToString(" › ") { it.name }

            // Si contenant, poids total récursif
            if (item.isContainer) {
                lifecycleScope.launch {
                    val total = vm.computeTotalWeight(itemId)
                    binding.textTotalWeight.visibility = View.VISIBLE
                    binding.textTotalWeight.text = "Poids total contenu : ${if (total >= 1000) "%.1f kg".format(total / 1000.0) else "$total g"}"
                }
            }

            // Rappels
            vm.getRemindersForItem(itemId).observe(viewLifecycleOwner) { reminders ->
                binding.containerReminders.removeAllViews()
                reminders.forEach { r ->
                    val chip = Chip(requireContext()).apply {
                        text = "🔔 ${r.label} (tous les ${r.intervalDays}j)"
                        setOnClickListener { vm.markReminderTriggered(r.id) }
                    }
                    binding.containerReminders.addView(chip)
                }
            }

// Historique mouvements
vm.getMovementsForItem(itemId).observe(viewLifecycleOwner) { logs ->
    binding.containerMovements.removeAllViews()

    logs.forEach { log ->
        val itemName = log.itemName.ifBlank { "Objet" }

        val action = when (log.action) {
            "ADD" -> {
                val destination = log.toContainerName.ifBlank { "Racine" }
                "➕ $itemName — Ajouté dans $destination"
            }

            "MOVE" -> {
                val origin = log.fromContainerName.ifBlank { "Racine" }
                val destination = log.toContainerName.ifBlank { "Racine" }
                "🔀 $itemName — Déplacé de $origin vers $destination"
            }

            "DELETE" -> {
                val origin = log.fromContainerName.ifBlank { "Racine" }
                "🗑️ $itemName — Supprimé depuis $origin"
            }

            else -> "$itemName — ${log.action}"
        }

        val tv = android.widget.TextView(requireContext()).apply {
            text = "${dateFormat.format(Date(log.timestamp))} — $action"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
            setPadding(0, 4, 0, 4)
        }

        binding.containerMovements.addView(tv)
    }
}
        }

        binding.buttonEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_detail_to_addItem,
                bundleOf("itemId" to itemId, "editMode" to true)
            )
        }

        binding.buttonQr.setOnClickListener {
            findNavController().navigate(R.id.action_detail_to_qr, bundleOf("itemId" to itemId))
        }

        binding.buttonGuidedMode.setOnClickListener {
            findNavController().navigate(R.id.action_detail_to_guided, bundleOf("itemId" to itemId))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
