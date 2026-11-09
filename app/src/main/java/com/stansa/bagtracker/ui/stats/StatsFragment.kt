package com.stansa.bagtracker.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.stansa.bagtracker.databinding.FragmentStatsBinding
import com.stansa.bagtracker.viewmodel.ItemViewModel
import com.stansa.bagtracker.viewmodel.StatsViewModel

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val itemViewModel: ItemViewModel by activityViewModels()
    private val statsViewModel: StatsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statsViewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.textCountObjects.text    = "Objets : ${stats.objectCount}"
            binding.textCountContainers.text = "Contenants : ${stats.containerCount}"
            binding.textTotalValue.text      = "Valeur totale : %.2f€".format(stats.totalValueCents / 100.0)
            binding.textExpired.text         = "Périmés : ${stats.expiredCount}"
            binding.textExpiringSoon.text    = "Périment bientôt : ${stats.expiringSoonCount}"
            binding.textNotSeen.text         = "Pas vus depuis 6 mois : ${stats.notSeenCount}"
            binding.textToReplace.text       = "À remplacer/cassés : ${stats.toReplaceCount}"

            setupPieChart(stats.tagDistribution)
        }
    }

    private fun setupPieChart(tagDist: Map<String, Int>) {
        val chart = binding.pieChart
        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.setHoleColor(Color.parseColor("#121220"))
        chart.holeRadius = 40f
        chart.setDrawEntryLabels(true)
        chart.legend.textColor = Color.WHITE

        val colors = listOf(
            "#6C5CE8", "#00CEC9", "#FDCB6E", "#E17055", "#55EFC4",
            "#FD79A8", "#74B9FF", "#A29BFE", "#FAB1A0", "#81ECEC"
        ).map { Color.parseColor(it) }

        if (tagDist.isEmpty()) {
            chart.visibility = View.GONE
            return
        }

        chart.visibility = View.VISIBLE
        val entries = tagDist.entries.mapIndexed { _, entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size)
            valueTextColor = Color.WHITE
            valueTextSize = 11f
            valueFormatter = PercentFormatter(chart)
        }
        chart.data = PieData(dataSet)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}