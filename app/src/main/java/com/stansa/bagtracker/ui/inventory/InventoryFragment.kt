package com.stansa.bagtracker.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.stansa.bagtracker.R
import com.stansa.bagtracker.databinding.FragmentInventoryBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private val vm: ItemViewModel by activityViewModels()
    private lateinit var adapter: InventoryAdapter

    // pile de navigation hiérarchique
    private val parentStack = ArrayDeque<Long?>()
    private var currentParentId: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = InventoryAdapter(
            onClick = { iwt ->
                if (iwt.item.isContainer) {
                    parentStack.addLast(currentParentId)
                    currentParentId = iwt.item.id
                    observeCurrentLevel()
                    updateBreadcrumb()
                } else {
                    vm.touchItem(iwt.item.id)
                    findNavController().navigate(
                        R.id.action_inventory_to_detail,
                        bundleOf("itemId" to iwt.item.id)
                    )
                }
            },
            onLongClick = { iwt ->
                findNavController().navigate(
                    R.id.action_inventory_to_addItem,
                    bundleOf("itemId" to iwt.item.id, "editMode" to true)
                )
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                R.id.action_inventory_to_addItem,
                bundleOf("parentId" to currentParentId)
            )
        }

        binding.buttonBack.setOnClickListener {
            if (parentStack.isNotEmpty()) {
                currentParentId = parentStack.removeLast()
                observeCurrentLevel()
                updateBreadcrumb()
            }
        }

        binding.fabSearch.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_search)
        }

        observeCurrentLevel()
        observeTags()
        observeAlerts()
    }

    private fun observeCurrentLevel() {
        val source = if (currentParentId == null) vm.rootItems
        else vm.getChildrenOf(currentParentId!!)

        source.observe(viewLifecycleOwner) { adapter.submitList(it) }
        binding.buttonBack.visibility = if (currentParentId != null) View.VISIBLE else View.GONE
    }

    private fun updateBreadcrumb() {
        binding.textBreadcrumb.text = if (currentParentId == null) "🎒 Racine" else "..."
    }

    private fun observeTags() {
        vm.allTags.observe(viewLifecycleOwner) { tags ->
            binding.chipGroupFilter.removeAllViews()
            val allChip = Chip(requireContext()).apply {
                text = "Tous"
                isCheckable = true
                isChecked = true
                setOnClickListener { vm.filterByTag(null); observeCurrentLevel() }
            }
            binding.chipGroupFilter.addView(allChip)
            tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag.name
                    isCheckable = true
                    setOnClickListener { vm.filterByTag(tag.id); observeTagFiltered() }
                }
                binding.chipGroupFilter.addView(chip)
            }
        }
    }

    private fun observeTagFiltered() {
        vm.tagFilteredItems.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun observeAlerts() {
        vm.expiringSoon.observe(viewLifecycleOwner) { items ->
            binding.bannerExpiry.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
            binding.bannerExpiry.text = "⚠️ ${items.size} objet(s) périment dans 7 jours"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}