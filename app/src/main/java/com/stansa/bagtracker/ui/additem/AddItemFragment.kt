
package com.stansa.bagtracker.ui.additem

import kotlin.math.roundToInt
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.stansa.bagtracker.R
import com.stansa.bagtracker.data.Item
import com.stansa.bagtracker.data.Reminder
import com.stansa.bagtracker.data.Tag
import com.stansa.bagtracker.databinding.FragmentAddItemBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private val vm: ItemViewModel by activityViewModels()

    private var editItemId: Long = -1L
    private var parentId: Long? = null
    private var selectedExpiryMs: Long? = null
    private var photoPath: String? = null
    private var locationLat: Double? = null
    private var locationLng: Double? = null
    private var locationName: String? = null

    private val selectedTags = mutableListOf<Tag>()
    private val reminders = mutableListOf<Reminder>()

    private val dateFormat =
        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    private val pickImageRequest = 101

    private val conditionValues =
        listOf("neuf", "bon", "usé", "à remplacer", "cassé")

    private val unitValues =
        listOf("unités", "ml", "cl", "L", "g", "kg", "cm", "m", "doses")

    private var loadingItem = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editItemId = arguments?.getLong("itemId", -1L) ?: -1L
        parentId = arguments?.getLong("parentId", -1L)
            ?.takeIf { it != -1L }

        val isEdit = arguments?.getBoolean("editMode", false) ?: false
        val prefillBarcode = arguments?.getString("barcode")

        binding.spinnerCondition.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            conditionValues
        )

        binding.spinnerUnit.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            unitValues
        )

        prefillBarcode?.let {
            binding.editBarcode.setText(it)
        }

        binding.buttonDelete.visibility =
            if (isEdit) View.VISIBLE else View.GONE

        binding.buttonSave.isEnabled = !isEdit

        binding.buttonPickDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonPickPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, pickImageRequest)
        }

        binding.buttonGetLocation.setOnClickListener {
            grabLocation()
        }

        binding.buttonAddReminder.setOnClickListener {
            addReminderRow()
        }

        binding.buttonAddTag.setOnClickListener {
            addNewTag()
        }

        binding.buttonSave.setOnClickListener {
            save(isEdit)
        }

        binding.buttonDelete.setOnClickListener {
            lifecycleScope.launch {
                val item = vm.getItemById(editItemId)
                    ?: return@launch

                vm.deleteItem(item)
                findNavController().popBackStack()
            }
        }

        observeAllTags()

        if (isEdit && editItemId > 0L) {
            loadingItem = true
            loadForEdit(editItemId)
        } else {
            binding.buttonSave.isEnabled = true
        }
    }

    private fun loadForEdit(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val itemWithTags = vm.getItemWithTagsById(id)

                if (itemWithTags == null) {
                    Toast.makeText(
                        requireContext(),
                        "Objet introuvable",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                    return@launch
                }

                val item = itemWithTags.item

                binding.editName.setText(item.name)
                binding.editEmoji.setText(item.emoji)
                binding.editNotes.setText(item.notes)
                binding.editQuantity.setText(item.quantity.toString())
                binding.checkIsContainer.isChecked = item.isContainer
                binding.editBarcode.setText(item.barcode.orEmpty())

                binding.spinnerCondition.setSelection(
                    conditionValues.indexOf(item.condition).coerceAtLeast(0)
                )

                binding.spinnerUnit.setSelection(
                    unitValues.indexOf(item.quantityUnit).coerceAtLeast(0)
                )

                item.weightGrams?.let {
                    binding.editWeight.setText(it.toString())
                }

                item.volumeCm3?.let {
                    binding.editVolume.setText(it.toString())
                }

                item.priceCents?.let {
                    binding.editPrice.setText(
                        String.format(Locale.US, "%.2f", it / 100.0)
                    )
                }

                item.expirationDate?.let {
                    selectedExpiryMs = it
                    binding.textSelectedDate.text =
                        dateFormat.format(Date(it))
                }

                // Important : conserver le parent de l'objet modifié.
                parentId = item.parentId

                photoPath = item.photoPath
                photoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        Glide.with(this@AddItemFragment)
                            .load(file)
                            .into(binding.imagePreview)

                        binding.imagePreview.visibility = View.VISIBLE
                    }
                }

                locationLat = item.locationLat
                locationLng = item.locationLng
                locationName = item.locationName

                locationName?.let {
                    binding.textLocation.text = it
                }

                selectedTags.clear()
                selectedTags.addAll(itemWithTags.tags)

                reminders.clear()
                vm.getRemindersForItem(id)
                    .observe(viewLifecycleOwner) { list ->
                        reminders.clear()
                        reminders.addAll(list)
                        refreshReminderRows()
                    }

                refreshTagChips()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Impossible de charger l'objet : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingItem = false
                binding.buttonSave.isEnabled = true
            }
        }
    }

    private fun observeAllTags() {
        vm.allTags.observe(viewLifecycleOwner) {
            refreshTagChips()
        }
    }

    private fun refreshTagChips() {
        if (_binding == null) return

        binding.chipGroupAllTags.removeAllViews()

        val allTags = vm.allTags.value.orEmpty().toMutableList()

        selectedTags.forEach { selected ->
            if (allTags.none { it.name.equals(selected.name, true) }) {
                allTags.add(selected)
            }
        }

        allTags.distinctBy { it.name.lowercase(Locale.ROOT) }
            .forEach { tag ->

                val chip = Chip(requireContext()).apply {
                    text = tag.name
                    isCheckable = true
                    isChecked = selectedTags.any {
                        it.name.equals(tag.name, true)
                    }

                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            if (selectedTags.none {
                                    it.name.equals(tag.name, true)
                                }) {
                                selectedTags.add(tag)
                            }
                        } else {
                            selectedTags.removeAll {
                                it.name.equals(tag.name, true)
                            }
                        }
                    }
                }

                binding.chipGroupAllTags.addView(chip)
            }
    }

    private fun addNewTag() {
        val name = binding.editNewTag.text
            .toString()
            .trim()

        if (name.isBlank()) {
            binding.editNewTag.error = "Nom du tag requis"
            return
        }

        if (selectedTags.any {
                it.name.equals(name, ignoreCase = true)
            }) {
            Toast.makeText(
                requireContext(),
                "Ce tag est déjà sélectionné",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        selectedTags.add(Tag(name = name))
        binding.editNewTag.text?.clear()
        refreshTagChips()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        selectedExpiryMs?.let {
            calendar.timeInMillis = it
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(
                    year,
                    month,
                    day,
                    0,
                    0,
                    0
                )
                calendar.set(Calendar.MILLISECOND, 0)

                selectedExpiryMs = calendar.timeInMillis
                binding.textSelectedDate.text =
                    dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun grabLocation() {
        val manager = requireContext().getSystemService(
            android.content.Context.LOCATION_SERVICE
        ) as LocationManager

        try {
            val location =
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: manager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )

            if (location != null) {
                locationLat = location.latitude
                locationLng = location.longitude
                locationName = "%.5f, %.5f".format(
                    Locale.US,
                    location.latitude,
                    location.longitude
                )
                binding.textLocation.text = locationName
            } else {
                binding.textLocation.text = "Position non disponible"
            }
        } catch (e: SecurityException) {
            binding.textLocation.text = "Permission refusée"
        }
    }

    private fun addReminderRow() {
        reminders.add(
            Reminder(
                itemId = editItemId.coerceAtLeast(0L),
                label = "",
                intervalDays = 7
            )
        )
        refreshReminderRows()
    }

    private fun refreshReminderRows() {
        if (_binding == null) return

        binding.containerReminders.removeAllViews()

        reminders.forEachIndexed { index, reminder ->
            val row = layoutInflater.inflate(
                R.layout.row_reminder,
                binding.containerReminders,
                false
            )

            val editLabel =
                row.findViewById<android.widget.EditText>(
                    R.id.editReminderLabel
                )

            val editDays =
                row.findViewById<android.widget.EditText>(
                    R.id.editReminderDays
                )

            val removeButton =
                row.findViewById<android.widget.ImageButton>(
                    R.id.btnRemoveReminder
                )

            editLabel.setText(reminder.label)
            editDays.setText(reminder.intervalDays.toString())

            editLabel.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun afterTextChanged(
                        s: android.text.Editable?
                    ) {
                        if (index < reminders.size) {
                            reminders[index] = reminders[index].copy(
                                label = s?.toString().orEmpty()
                            )
                        }
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) = Unit
                }
            )

            editDays.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun afterTextChanged(
                        s: android.text.Editable?
                    ) {
                        if (index < reminders.size) {
                            val days = s?.toString()
                                ?.toIntOrNull()
                                ?.coerceAtLeast(1)
                                ?: 7

                            reminders[index] = reminders[index].copy(
                                intervalDays = days
                            )
                        }
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) = Unit
                }
            )

            removeButton.setOnClickListener {
                if (index < reminders.size) {
                    reminders.removeAt(index)
                    refreshReminderRows()
                }
            }

            binding.containerReminders.addView(row)
        }
    }

    private fun save(isEdit: Boolean) {
        if (loadingItem) return

        val name = binding.editName.text
            .toString()
            .trim()

        if (name.isEmpty()) {
            binding.editName.error = "Nom requis"
            return
        }

        val quantity = binding.editQuantity.text
            .toString()
            .toIntOrNull()

        if (quantity == null || quantity <= 0) {
            binding.editQuantity.error =
                "La quantité doit être supérieure à zéro"
            return
        }
val priceText = binding.editPrice.text.toString().trim()
val price = priceText.toDoubleOrNull()

if (
    priceText.isNotEmpty() &&
    (
        price == null ||
        !price.isFinite() ||
        price < 0.0 ||
        price > Int.MAX_VALUE / 100.0
    )
) {
    binding.editPrice.error = "Prix invalide ou trop élevé"
    return
}

val priceCents = price?.let {
    (it * 100.0).roundToInt()
}

    val weightText = binding.editWeight.text.toString().trim()
val weight = if (weightText.isEmpty()) {
    null
} else {
    weightText.toIntOrNull()
}

if (weightText.isNotEmpty() && (weight == null || weight < 0)) {
    binding.editWeight.error = "Poids invalide"
    return
}

val volumeText = binding.editVolume.text.toString().trim()
val volume = if (volumeText.isEmpty()) {
    null
} else {
    volumeText.toIntOrNull()
}

if (volumeText.isNotEmpty() && (volume == null || volume < 0)) {
    binding.editVolume.error = "Volume invalide"
    return
}

        val item = Item(
            id = if (isEdit) editItemId else 0L,
            name = name,
            emoji = binding.editEmoji.text
                .toString()
                .trim()
                .ifEmpty { "📦" },
            isContainer = binding.checkIsContainer.isChecked,
            quantity = quantity,
            quantityUnit = unitValues[
                binding.spinnerUnit.selectedItemPosition
                    .coerceIn(unitValues.indices)
            ],
            condition = conditionValues[
                binding.spinnerCondition.selectedItemPosition
                    .coerceIn(conditionValues.indices)
            ],
            notes = binding.editNotes.text.toString(),
            parentId = parentId,
            expirationDate = selectedExpiryMs,
            photoPath = photoPath,
            weightGrams = weight,
            volumeCm3 = volume,
            priceCents = priceCents,
            locationLat = locationLat,
            locationLng = locationLng,
            locationName = locationName,
            barcode = binding.editBarcode.text
                .toString()
                .trim()
                .ifEmpty { null }
        )

        val validReminders = reminders
            .filter { it.label.isNotBlank() }
            .map {
                it.copy(
                    intervalDays = it.intervalDays.coerceAtLeast(1)
                )
            }
binding.buttonSave.isEnabled = false

lifecycleScope.launch {
    if (
        isEdit &&
        vm.wouldCreateParentCycle(editItemId, parentId)
    ) {
        Toast.makeText(
            requireContext(),
            "Impossible de placer un élément dans son propre contenu.",
            Toast.LENGTH_LONG
        ).show()

        binding.buttonSave.isEnabled = true
        return@launch
    }

    if (isEdit) {
        vm.updateItem(item, selectedTags, validReminders)
    } else {
        vm.upsertItem(item, selectedTags, validReminders)
    }

    findNavController().popBackStack()
}
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickImageRequest &&
            resultCode == Activity.RESULT_OK
        ) {
            val uri: Uri = data?.data ?: return
            val id = if (editItemId > 0L) {
                editItemId
            } else {
                System.currentTimeMillis()
            }

            photoPath = vm.savePhoto(uri, id)

            photoPath?.let { path ->
                Glide.with(this)
                    .load(File(path))
                    .into(binding.imagePreview)

                binding.imagePreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
