package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.oneshu.applistfilter.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppScopeAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import android.widget.Toast

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
            AppScopeAdapter(selectedPackageNames, null, ::onSelectionChanged)
        } else {
            AppScopeAdapter(
                selectedPackageNames,
                { ConfigManager.getAppConfig(it)?.useWhitelist == args.isWhitelist },
                ::onSelectionChanged
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
        val args by navArgs<ScopeFragmentArgs>()
        binding.saveAsTemplate.visibility =
            if (args.allowSaveAsTemplate) View.VISIBLE else View.GONE
        binding.saveAsTemplate.setOnClickListener { showSaveTemplateDialog() }
        onAppCategoryChanged(binding.categoryTabs.selectedTabPosition == 1)
        updateSelectionSummary()
    }

    override fun onAppCategoryChanged(showSystemApps: Boolean) {
        val args by navArgs<ScopeFragmentArgs>()
        val lockSystemApps = showSystemApps && args.isWhitelist && args.systemAppsAllowed
        (adapter as AppScopeAdapter).setSelectionLocked(lockSystemApps)
        binding.selectionNoticeCard.visibility = if (lockSystemApps) View.VISIBLE else View.GONE
        updateSelectionSummary()
    }

    private fun onSelectionChanged() {
        updateSelectionSummary()
        sortList()
    }

    private fun updateSelectionSummary() {
        binding.selectionSummary.text =
            getString(R.string.selection_count, selectedPackageNames.size)
        val args by navArgs<ScopeFragmentArgs>()
        val systemAppsLocked = binding.categoryTabs.selectedTabPosition == 1 &&
                args.isWhitelist && args.systemAppsAllowed
        binding.clearSelection.isEnabled = selectedPackageNames.isNotEmpty() && !systemAppsLocked
    }

    private fun finishSelection() {
        setFragmentResult("app_select", Bundle().apply {
            putStringArrayList("selectedPackages", ArrayList(selectedPackageNames))
        })
        navController.navigateUp()
    }

    private fun showSaveTemplateDialog() {
        val context = requireContext()
        val input = TextInputEditText(context).apply {
            hint = getString(R.string.app_save_template_hint)
            setSingleLine(true)
        }
        val inputLayout = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 3, padding, 0)
            addView(input)
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.app_save_template_title)
            .setView(inputLayout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text?.toString()?.trim().orEmpty()
                when {
                    name.isEmpty() -> inputLayout.error = getString(R.string.template_name_empty)
                    ConfigManager.hasTemplate(name) ->
                        inputLayout.error = getString(R.string.template_name_already_exist)
                    else -> {
                        val args by navArgs<ScopeFragmentArgs>()
                        ConfigManager.saveTemplate(
                            originalName = null,
                            name = name,
                            isWhitelist = args.isWhitelist,
                            templatePackages = selectedPackageNames.toSet()
                        )
                        Toast.makeText(
                            context,
                            R.string.app_save_template_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
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
