package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppScopeAdapter
import icu.nullptr.hidemyapplist.ui.util.navController

class ScopeFragment : AppSelectFragment() {

    private lateinit var selectedPackageNames: MutableSet<String>
    private lateinit var initialSelection: Set<String>

    override val primaryComparator: Comparator<String> =
        Comparator.comparing { it !in selectedPackageNames }

    override val adapter by lazy {
        val args by navArgs<ScopeFragmentArgs>()
        selectedPackageNames = args.selectedPackages.toMutableSet()
        initialSelection = selectedPackageNames.toSet()
        if (!args.filterOnlyEnabled) {
            AppScopeAdapter(selectedPackageNames, null, ::updateSelectionSummary)
        } else {
            AppScopeAdapter(
                selectedPackageNames,
                { ConfigManager.getAppConfig(it)?.useWhitelist == args.isWhitelist },
                ::updateSelectionSummary
            )
        }
    }

    override fun getToolbarTitle(): CharSequence {
        val args by navArgs<ScopeFragmentArgs>()
        return args.title
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.selectionActionBar.visibility = View.VISIBLE
        binding.selectionSummary.visibility = View.VISIBLE
        binding.finishSelection.setOnClickListener { finishSelection() }
        binding.clearSelection.setOnClickListener {
            (adapter as AppScopeAdapter).clearSelection()
        }
        updateSelectionSummary()
    }

    private fun updateSelectionSummary() {
        binding.selectionSummary.text =
            getString(R.string.selection_count, selectedPackageNames.size)
        binding.clearSelection.isEnabled = selectedPackageNames.isNotEmpty()
    }

    private fun finishSelection() {
        setFragmentResult("app_select", Bundle().apply {
            putStringArrayList("selectedPackages", ArrayList(selectedPackageNames))
        })
        navController.navigateUp()
    }

    override fun onBack() {
        if (selectedPackageNames == initialSelection) {
            navController.navigateUp()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.selection_discard_title)
            .setMessage(R.string.selection_discard_message)
            .setPositiveButton(R.string.discard_selection) { _, _ -> navController.navigateUp() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
