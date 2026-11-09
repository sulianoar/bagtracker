
package com.stansa.bagtracker.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.stansa.bagtracker.R
import com.stansa.bagtracker.data.ItemWithTags
import com.stansa.bagtracker.databinding.FragmentInventoryBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val vm: ItemViewModel by activityViewModels()
    private lateinit var adapter: InventoryAdapter

    private var currentParentId: Long? = null
    private var currentItems: List<ItemWithTags> = emptyList()
    private var selectedTagId: Long? = null

    private var currentSource: LiveData<List<ItemWithTags>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentParentId = arguments?.getLong("parentId", -1L)
            ?.takeIf { it > 0L }

        adapter = InventoryAdapter(
            onClick = { itemWithTags ->
                val item = itemWithTags.item

                if (item.isContainer) {
                    currentParentId = item.id
                    selectedTagId = null
                    observeCurrentLevel()
                    updateBreadcrumb()
                    updateBackButton()
                } else {
                    vm.touchItem(item.id)

                    findNavController().navigate(
                        R.id.action_inventory_to_detail,
                        bundleOf("itemId" to item.id)
                    )
                }
            },
            onLongClick = { itemWithTags ->
                findNavController().navigate(
                    R.id.action_inventory_to_addItem,
                    bundleOf(
                        "itemId" to itemWithTags.item.id,
                        "editMode" to true
                    )
                )
            }
        )

        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                R.id.action_inventory_to_addItem,
                bundleOf(
                    "parentId" to (currentParentId ?: -1L)
                )
            )
        }

        binding.buttonBack.setOnClickListener {
            goToParent()
        }

        binding.fabSearch.setOnClickListener {
            findNavController().navigate(
                R.id.action_inventory_to_search
            )
        }

        observeTags()
        observeAlerts()
        observeCurrentLevel()
        updateBreadcrumb()
        updateBackButton()
    }

    private fun observeCurrentLevel() {
        currentSource?.removeObservers(viewLifecycleOwner)

        val source = currentParentId?.let {
            vm.getChildrenOf(it)
        } ?: vm.rootItems

        currentSource = source

        source.observe(viewLifecycleOwner) { items ->
            currentItems = items
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        val filtered = selectedTagId?.let { tagId ->
            currentItems.filter { itemWithTags ->
                itemWithTags.tags.any { it.id == tagId }
            }
        } ?: currentItems

        adapter.submitList(filtered)

    }

    private fun observeTags() {
        vm.allTags.observe(viewLifecycleOwner) { tags ->
            if (_binding == null) return@observe

            binding.chipGroupFilter.removeAllViews()

            val allChip = Chip(requireContext()).apply {
                text = "Tous"
                isCheckable = true
                isChecked = selectedTagId == null

                setOnClickListener {
                    selectedTagId = null
                    updateTagChipSelection()
                    applyCurrentFilter()
                }
            }

            binding.chipGroupFilter.addView(allChip)

            tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag.name
                    isCheckable = true
                    isChecked = selectedTagId == tag.id

                    setOnClickListener {
                        selectedTagId = tag.id
                        updateTagChipSelection()
                        applyCurrentFilter()
                    }
                }

                binding.chipGroupFilter.addView(chip)
            }
        }
    }

    private fun updateTagChipSelection() {
        for (index in 0 until binding.chipGroupFilter.childCount) {
            val chip = binding.chipGroupFilter.getChildAt(index) as? Chip
                ?: continue

            chip.isChecked = if (index == 0) {
                selectedTagId == null
            } else {
                val tagName = chip.text.toString()
                vm.allTags.value
                    ?.firstOrNull { it.name == tagName }
                    ?.id == selectedTagId
            }
        }
    }

    private fun goToParent() {
        val parentId = currentParentId ?: return

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val path = vm.getFullPath(parentId)

            currentParentId = path
                .dropLast(1)
                .lastOrNull()
                ?.id

            selectedTagId = null
            observeCurrentLevel()
            updateBreadcrumb()
            updateBackButton()
        }
    }

    private fun updateBreadcrumb() {
        val id = currentParentId

        if (id == null) {
            binding.textBreadcrumb.text = "🎒 Racine"
            return
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val path = vm.getFullPath(id)

            if (_binding != null) {
                binding.textBreadcrumb.text =
                    "🎒 " + path.joinToString("  ›  ") { it.name }
            }
        }
    }

    private fun updateBackButton() {
        binding.buttonBack.visibility =
            if (currentParentId == null) View.GONE else View.VISIBLE
    }

    private fun observeAlerts() {
        vm.expiringSoon.observe(viewLifecycleOwner) { items ->
            if (_binding == null) return@observe

            binding.bannerExpiry.visibility =
                if (items.isNotEmpty()) View.VISIBLE else View.GONE

            binding.bannerExpiry.text =
                "⚠️ ${items.size} objet(s) périment dans 7 jours"
        }
    }

    override fun onDestroyView() {
        currentSource?.removeObservers(viewLifecycleOwner)
        currentSource = null
        _binding = null
        super.onDestroyView()
    }
}
