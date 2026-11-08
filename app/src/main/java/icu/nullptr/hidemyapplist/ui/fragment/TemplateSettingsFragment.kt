package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentTemplateSettingsBinding
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.TemplateSettingsViewModel
import kotlinx.coroutines.launch

class TemplateSettingsFragment : Fragment(R.layout.fragment_template_settings) {

    private val binding by viewBinding<FragmentTemplateSettingsBinding>()
    private val viewModel by viewModels<TemplateSettingsViewModel> {
        val args by navArgs<TemplateSettingsFragmentArgs>()
        TemplateSettingsViewModel.Factory(args)
    }

    private fun showInvalidName(@StringRes message: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_name_invalid)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun saveTemplate() {
        viewModel.name = viewModel.name?.trim()
        val name = viewModel.name
        if (name.isNullOrEmpty()) {
            showInvalidName(R.string.template_name_empty)
            return
        }
        if (name != viewModel.originalName && ConfigManager.hasTemplate(name)) {
            showInvalidName(R.string.template_name_already_exist)
            return
        }
        saveResult(false)
    }

    private fun confirmDiscard() {
        viewModel.name = viewModel.name?.trim()
        if (!viewModel.hasUnsavedChanges()) {
            navController.navigateUp()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_discard_title)
            .setMessage(R.string.template_discard_message)
            .setPositiveButton(R.string.template_discard) { _, _ -> navController.navigateUp() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_delete_title)
            .setMessage(R.string.template_delete)
            .setPositiveButton(android.R.string.ok) { _, _ -> saveResult(true) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveResult(delete: Boolean) {
        setFragmentResult("template_settings", Bundle().apply {
            putString("name", if (delete) null else viewModel.name)
            putStringArrayList("appliedPackages", viewModel.appliedPackages.value)
            putStringArrayList("templatePackages", viewModel.templatePackages.value)
        })
        navController.navigateUp()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { confirmDiscard() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = viewModel.originalName ?: getString(R.string.template_create),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { confirmDiscard() },
            menuRes = R.menu.menu_delete,
            onMenuOptionSelected = { item ->
                when (item.itemId) {
                    R.id.menu_save -> saveTemplate()
                    R.id.menu_delete -> confirmDelete()
                }
            }
        )
        binding.toolbar.menu.findItem(R.id.menu_delete)?.isVisible = viewModel.originalName != null

        binding.templateName.setText(viewModel.name)
        binding.templateName.addTextChangedListener { viewModel.name = it.toString() }
        binding.templateType.text = getString(
            if (viewModel.isWhitelist) R.string.whitelist else R.string.blacklist
        )
        binding.templatePackages.text = getString(
            if (viewModel.isWhitelist) R.string.template_target_whitelist_title
            else R.string.template_target_blacklist_title
        )
        binding.appliedPackages.text = getString(R.string.template_scope_title)
        binding.templatePackages.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.templatePackages.value =
                    bundle.getStringArrayList("selectedPackages")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = false,
                selectedPackages = viewModel.templatePackages.value.toTypedArray(),
                title = getString(
                    if (viewModel.isWhitelist) R.string.template_target_whitelist_picker_title
                    else R.string.template_target_blacklist_picker_title
                )
            )
            navController.navigate(R.id.nav_scope, args.toBundle())
        }
        binding.appliedPackages.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.appliedPackages.value =
                    bundle.getStringArrayList("selectedPackages")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = true,
                isWhitelist = viewModel.isWhitelist,
                selectedPackages = viewModel.appliedPackages.value.toTypedArray(),
                title = getString(R.string.template_scope_picker_title)
            )
            navController.navigate(R.id.nav_scope, args.toBundle())
        }

        lifecycleScope.launch {
            viewModel.templatePackages.collect {
                val fmt =
                    if (viewModel.isWhitelist) R.string.template_apps_visible_count
                    else R.string.template_apps_invisible_count
                binding.templatePackages.summary = String.format(getString(fmt), it.size)
            }
        }
        lifecycleScope.launch {
            viewModel.appliedPackages.collect {
                binding.appliedPackages.summary = if (it.isEmpty()) {
                    getString(R.string.template_not_applied)
                } else {
                    String.format(getString(R.string.template_applied_count), it.size)
                }
            }
        }
    }
}
