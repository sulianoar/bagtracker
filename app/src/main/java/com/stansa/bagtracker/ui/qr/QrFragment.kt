
package com.stansa.bagtracker.ui.qr

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.stansa.bagtracker.databinding.FragmentQrBinding
import com.stansa.bagtracker.util.QrUtil
import com.stansa.bagtracker.viewmodel.ItemViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QrFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!

    private val vm: ItemViewModel by activityViewModels()

    private var qrBitmap: Bitmap? = null
    private var currentItemId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentItemId = arguments?.getLong("itemId", -1L) ?: -1L

        if (currentItemId <= 0L) {
            Toast.makeText(
                requireContext(),
                "Identifiant d'objet invalide",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        binding.buttonSaveQr.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val item = vm.getItemById(currentItemId)

                if (item == null) {
                    Toast.makeText(
                        requireContext(),
                        "Objet introuvable",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val content = QrUtil.buildQrContent(
                    item.id,
                    item.name
                )

                qrBitmap = withContext(Dispatchers.Default) {
                    QrUtil.generateQrBitmap(content)
                }

                binding.imageQr.setImageBitmap(qrBitmap)
                binding.textQrContent.text = content
                binding.textQrTitle.text = "QR Code : ${item.name}"
                binding.buttonSaveQr.isEnabled = true

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur de génération du QR code : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.buttonSaveQr.setOnClickListener {
            saveQrCode()
        }
    }

    private fun saveQrCode() {
        val bitmap = qrBitmap ?: return

        binding.buttonSaveQr.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(
                        requireContext().filesDir,
                        "qr_codes"
                    )

                    if (!directory.exists() && !directory.mkdirs()) {
                        throw IllegalStateException(
                            "Impossible de créer le dossier QR"
                        )
                    }

                    val output = File(
                        directory,
                        "item_$currentItemId.png"
                    )

                    FileOutputStream(output).use { stream ->
                        if (!bitmap.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                stream
                            )
                        ) {
                            throw IllegalStateException(
                                "Échec de l'enregistrement PNG"
                            )
                        }

                        stream.flush()
                    }

                    output
                }

                Toast.makeText(
                    requireContext(),
                    "QR enregistré : ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur d'enregistrement : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.buttonSaveQr.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        binding.imageQr.setImageDrawable(null)
        qrBitmap?.recycle()
        qrBitmap = null

        _binding = null
        super.onDestroyView()
    }
}
