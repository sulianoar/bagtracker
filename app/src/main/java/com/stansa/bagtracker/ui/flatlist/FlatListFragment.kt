package com.stansa.bagtracker.ui.flat

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.stansa.bagtracker.R
import com.stansa.bagtracker.databinding.FragmentFlatListBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel

class FlatListFragment : Fragment() {

    private var _binding: FragmentFlatListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemViewModel by activityViewModels()
    private lateinit var adapter: FlatListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFlatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FlatListAdapter { itemWithTags ->
            val action = FlatListFragmentDirections.actionFlatListToDetail(itemWithTags.item.id)
            findNavController().navigate(action)
        }
        binding.recyclerFlat.adapter = adapter

        val sortOptions = listOf("Nom", "Péremption", "Valeur", "Poids", "Dernière vue")
        val sortKeys   = listOf("name", "expiry", "value", "weight", "lastSeen")

        binding.spinnerSort.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            sortOptions
        )

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                viewModel.setSortMode(sortKeys[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        viewModel.flatItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.itemsToReplace.observe(viewLifecycleOwner) { items ->
            binding.bannerReplace.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
            binding.bannerReplace.text = "⚠️ ${items.size} objet(s) à remplacer"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}