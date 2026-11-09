package com.stansa.bagtracker.ui.search

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stansa.bagtracker.R
import com.stansa.bagtracker.databinding.FragmentSearchBinding
import com.stansa.bagtracker.ui.inventory.InventoryAdapter
import com.stansa.bagtracker.viewmodel.ItemViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val vm: ItemViewModel by activityViewModels()
    private lateinit var adapter: InventoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = InventoryAdapter(
            onClick = { iwt ->
                vm.touchItem(iwt.item.id)
                findNavController().navigate(R.id.action_search_to_detail, bundleOf("itemId" to iwt.item.id))
            },
            onLongClick = { iwt ->
                findNavController().navigate(R.id.action_search_to_addItem,
                    bundleOf("itemId" to iwt.item.id, "editMode" to true))
            }
        )

        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter

        binding.editSearch.addTextChangedListener { vm.search(it.toString()) }

        vm.searchResults.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.textEmpty.visibility = if (it.isEmpty() && binding.editSearch.text.isNotEmpty())
                View.VISIBLE else View.GONE
        }

        binding.buttonScanBarcode.setOnClickListener {
            findNavController().navigate(R.id.action_search_to_barcode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}