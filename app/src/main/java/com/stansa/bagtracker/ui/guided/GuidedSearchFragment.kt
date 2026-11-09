package com.stansa.bagtracker.ui.guided

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stansa.bagtracker.databinding.FragmentGuidedModeBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class GuidedModeFragment : Fragment() {

    private var _binding: FragmentGuidedModeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ItemViewModel by activityViewModels()
    private val args: GuidedModeFragmentArgs by navArgs()

    private var steps: List<String> = emptyList()
    private var currentStep = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGuidedModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val path = viewModel.getFullPath(args.itemId)
            if (path.isEmpty()) {
                binding.textInstruction.text = "Chemin introuvable."
                binding.btnNext.isEnabled = false
                return@launch
            }

            val item = viewModel.getItemById(args.itemId)
            val labels = path.map { it.name }.toMutableList()

            steps = buildList {
                for (i in 0 until labels.size - 1) {
                    add("📂 Ouvre : ${labels[i]}")
                }
                add("✅ C'est là : ${item?.emoji ?: "📦"} ${item?.name ?: ""}")
            }

            currentStep = 0
            showStep()

            binding.btnNext.setOnClickListener {
                if (currentStep < steps.size - 1) {
                    currentStep++
                    showStep()
                } else {
                    viewModel.touchItem(args.itemId)
                    findNavController().popBackStack()
                }
            }

            binding.btnPrev.setOnClickListener {
                if (currentStep > 0) {
                    currentStep--
                    showStep()
                }
            }
        }
    }

    private fun showStep() {
        binding.textInstruction.text = steps[currentStep]
        binding.textProgress.text = "${currentStep + 1} / ${steps.size}"
        binding.btnPrev.isEnabled = currentStep > 0
        binding.btnNext.text = if (currentStep == steps.size - 1) "Terminé" else "Suivant →"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}