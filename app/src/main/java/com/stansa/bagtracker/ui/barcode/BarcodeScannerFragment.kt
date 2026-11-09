
package com.stansa.bagtracker.ui.barcode

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import com.stansa.bagtracker.R
import com.stansa.bagtracker.databinding.FragmentBarcodeScannerBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

class BarcodeScannerFragment : Fragment() {

    private var _binding: FragmentBarcodeScannerBinding? = null
    private val binding get() = _binding!!

    private val vm: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeScannerBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStartScan.setOnClickListener {
            startScanner()
        }

        // Démarrage automatique à l'ouverture de l'écran.
        startScanner()
    }

    private fun startScanner() {
        if (!isAdded) return

        binding.textScanStatus.text = "En attente du scan..."

        IntentIntegrator.forSupportFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            .setPrompt("Placez le code-barres dans le cadre")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .initiateScan()
    }

    @Deprecated("Utilisé par l'intégration ZXing")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(
            requestCode,
            resultCode,
            data
        ) ?: return

        if (result.contents.isNullOrBlank()) {
            binding.textScanStatus.text = "Scan annulé."
            return
        }

        val barcode = result.contents.trim()
        binding.textScanStatus.text = "Code détecté : $barcode"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val item = vm.getItemByBarcode(barcode)

                if (item != null) {
                    findNavController().navigate(
                        R.id.action_barcode_to_detail,
                        bundleOf("itemId" to item.id)
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_barcode_to_addItem,
                        bundleOf(
                            "itemId" to -1L,
                            "barcode" to barcode
                        )
                    )
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur lors de la recherche : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
