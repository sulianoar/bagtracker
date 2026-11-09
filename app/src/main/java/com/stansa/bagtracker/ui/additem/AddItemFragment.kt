package com.stansa.bagtracker.ui.additem

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.stansa.bagtracker.R
import com.stansa.bagtracker.data.*
import com.stansa.bagtracker.databinding.FragmentAddItemBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    private val PICK_IMAGE = 101

    private val conditionValues = listOf("neuf", "bon", "usé", "à remplacer", "cassé")
    private val unitValues = listOf("unités", "ml", "cl", "L", "g", "kg", "cm", "m", "doses")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editItemId = arguments?.getLong("itemId", -1L) ?: -1L
        parentId = arguments?.getLong("parentId", -1L).takeIf { it != -1L }
        val isEdit = arguments?.getBoolean("editMode", false) ?: false
        val prefillBarcode = arguments?.getString("barcode")

        // Spinners
        binding.spinnerCondition.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, conditionValues)
        binding.spinnerUnit.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, unitValues)

        prefillBarcode?.let { binding.editBarcode.setText(it) }

        if (isEdit && editItemId != -1L) {
            loadForEdit(editItemId)
            binding.buttonDelete.visibility = View.VISIBLE
        } else {
            binding.buttonDelete.visibility = View.GONE
        }

        binding.buttonPickDate.setOnClickListener { showDatePicker() }
        binding.buttonPickPhoto.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, PICK_IMAGE)
        }
        binding.buttonGetLocation.setOnClickListener { grabLocation() }

        binding.buttonAddReminder.setOnClickListener { addReminderRow() }

        binding.buttonSave.setOnClickListener { save(isEdit) }
        binding.buttonDelete.setOnClickListener {
            lifecycleScope.launch {
                val item = vm.getItemById(editItemId) ?: return@launch
                vm.deleteItem(item)
                findNavController().popBackStack()
            }
        }

        observeAllTags()
    }

    private fun loadForEdit(id: Long) {
        lifecycleScope.launch {
            val iwt = vm.getItemWithTagsById(id) ?: return@launch
            val item = iwt.item
            binding.editName.setText(item.name)
            binding.editEmoji.setText(item.emoji)
            binding.editNotes.setText(item.notes)
            binding.editQuantity.setText(item.quantity.toString())
            binding.checkIsContainer.isChecked = item.isContainer
            binding.editBarcode.setText(item.barcode ?: "")
            binding.spinnerCondition.setSelection(conditionValues.indexOf(item.condition).coerceAtLeast(0))
            binding.spinnerUnit.setSelection(unitValues.indexOf(item.quantityUnit).coerceAtLeast(0))
            item.weightGrams?.let { binding.editWeight.setText(it.toString()) }
            item.volumeCm3?.let { binding.editVolume.setText(it.toString()) }
            item.priceCents?.let { binding.editPrice.setText((it / 100.0).toString()) }
            item.expirationDate?.let {
                selectedExpiryMs = it
                binding.textSelectedDate.text = dateFormat.format(Date(it))
            }
            photoPath = item.photoPath
            photoPath?.let { path ->
                if (File(path).exists()) {
                    Glide.with(this@AddItemFragment).load(File(path)).into(binding.imagePreview)
                    binding.imagePreview.visibility = View.VISIBLE
                }
            }
            locationLat = item.locationLat
            locationLng = item.locationLng
            locationName = item.locationName
            locationName?.let { binding.textLocation.text = it }

            selectedTags.clear()
            selectedTags.addAll(iwt.tags)

            vm.getRemindersForItem(id).observe(viewLifecycleOwner) { list ->
                reminders.clear()
                reminders.addAll(list)
                refreshReminderRows()
            }
        }
    }

    private fun observeAllTags() {
        vm.allTags.observe(viewLifecycleOwner) { allTags ->
            binding.chipGroupAllTags.removeAllViews()
            allTags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag.name
                    isCheckable = true
                    isChecked = selectedTags.any { it.id == tag.id }
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) { if (!selectedTags.contains(tag)) selectedTags.add(tag) }
                        else selectedTags.remove(tag)
                    }
                }
                binding.chipGroupAllTags.addView(chip)
            }
            // Champ texte pour nouveau tag
            binding.buttonAddTag.setOnClickListener {
                val name = binding.editNewTag.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newTag = Tag(name = name)
                    selectedTags.add(newTag)
                    binding.editNewTag.setText("")
                    observeAllTags()
                }
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            selectedExpiryMs = cal.timeInMillis
            binding.textSelectedDate.text = dateFormat.format(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun grabLocation() {
        val lm = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                locationLat = loc.latitude
                locationLng = loc.longitude
                locationName = "%.5f, %.5f".format(loc.latitude, loc.longitude)
                binding.textLocation.text = locationName
            } else {
                binding.textLocation.text = "Position non disponible"
            }
        } catch (e: SecurityException) {
            binding.textLocation.text = "Permission refusée"
        }
    }

    private fun addReminderRow() {
        reminders.add(Reminder(itemId = 0, label = "", intervalDays = 7))
        refreshReminderRows()
    }

    private fun refreshReminderRows() {
        binding.containerReminders.removeAllViews()
        reminders.forEachIndexed { index, reminder ->
            val row = layoutInflater.inflate(R.layout.row_reminder, binding.containerReminders, false)
            val editLabel = row.findViewById<android.widget.EditText>(R.id.editReminderLabel)
            val editDays = row.findViewById<android.widget.EditText>(R.id.editReminderDays)
            val btnRemove = row.findViewById<android.widget.ImageButton>(R.id.btnRemoveReminder)
            editLabel.setText(reminder.label)
            editDays.setText(reminder.intervalDays.toString())
            editLabel.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    reminders[index] = reminders[index].copy(label = s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            editDays.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    reminders[index] = reminders[index].copy(intervalDays = s.toString().toIntOrNull() ?: 7)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            btnRemove.setOnClickListener { reminders.removeAt(index); refreshReminderRows() }
            binding.containerReminders.addView(row)
        }
    }

    private fun save(isEdit: Boolean) {
        val name = binding.editName.text.toString().trim()
        if (name.isEmpty()) { binding.editName.error = "Nom requis"; return }

        val item = Item(
            id = if (isEdit) editItemId else 0,
            name = name,
            emoji = binding.editEmoji.text.toString().trim().ifEmpty { "📦" },
            isContainer = binding.checkIsContainer.isChecked,
            quantity = binding.editQuantity.text.toString().toIntOrNull() ?: 1,
            quantityUnit = unitValues[binding.spinnerUnit.selectedItemPosition],
            condition = conditionValues[binding.spinnerCondition.selectedItemPosition],
            notes = binding.editNotes.text.toString(),
            parentId = parentId,
            expirationDate = selectedExpiryMs,
            photoPath = photoPath,
            weightGrams = binding.editWeight.text.toString().toIntOrNull(),
            volumeCm3 = binding.editVolume.text.toString().toIntOrNull(),
            priceCents = binding.editPrice.text.toString().toDoubleOrNull()?.let { (it * 100).toInt() },
            locationLat = locationLat,
            locationLng = locationLng,
            locationName = locationName,
            barcode = binding.editBarcode.text.toString().trim().ifEmpty { null }
        )

        val validReminders = reminders.filter { it.label.isNotBlank() }

        if (isEdit) vm.updateItem(item, selectedTags, validReminders)
        else vm.upsertItem(item, selectedTags, validReminders)

        findNavController().popBackStack()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val id = if (editItemId != -1L) editItemId else System.currentTimeMillis()
            photoPath = vm.savePhoto(uri, id)
            photoPath?.let {
                Glide.with(this).load(File(it)).into(binding.imagePreview)
                binding.imagePreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}