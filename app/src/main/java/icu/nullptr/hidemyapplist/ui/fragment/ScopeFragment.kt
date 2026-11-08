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

    private lateinit var checked: MutableSet<String>
    private lateinit var initialChecked: Set<String>

    override val firstComparator: Comparator<String> = Comparator.comparing { !checked.contains(it) }

    override val adapter by lazy {
        val args by navArgs<ScopeFragmentArgs>()
        checked = args.checked.toMutableSet()
        initialChecked = checked.toSet()
        if (!args.filterOnlyEnabled) AppScopeAdapter(checked, null, ::updateSelectionSummary)
        else AppScopeAdapter(
            checked,
            { ConfigManager.getAppConfig(it)?.useWhitelist == args.isWhiteList },
            ::updateSelectionSummary
        )
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
        binding.selectionSummary.text = getString(R.string.selection_count, checked.size)
        binding.clearSelection.isEnabled = checked.isNotEmpty()
    }

    private fun finishSelection() {
        setFragmentResult("app_select", Bundle().apply {
            putStringArrayList("checked", ArrayList(checked))
        })
        navController.navigateUp()
    }

    override fun onBack() {
        if (checked == initialChecked) {
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
