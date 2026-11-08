package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentTemplateManageBinding
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.TemplateAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar

class TemplateManageFragment : Fragment(R.layout.fragment_template_manage) {

    private val binding by viewBinding<FragmentTemplateManageBinding>()
    private val adapter = TemplateAdapter(this::navigateToSettings)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_template_manage),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() }
        )

        binding.newTemplate.setOnClickListener {
            showTemplateTypeDialog()
        }
        binding.templateList.layoutManager = LinearLayoutManager(context)
        binding.templateList.adapter = adapter
    }

    private fun showTemplateTypeDialog() {
        var isWhitelist = false
        val options = arrayOf(
            getString(R.string.template_type_blacklist_option),
            getString(R.string.template_type_whitelist_option)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_type_choose_title)
            .setSingleChoiceItems(options, 0) { _, which ->
                isWhitelist = which == 1
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.next) { _, _ ->
                navigateToSettings(ConfigManager.TemplateInfo(null, isWhitelist))
            }
            .show()
    }

    private fun navigateToSettings(info: ConfigManager.TemplateInfo) {
        setFragmentResultListener("template_settings") { _, bundle ->
            val name = bundle.getString("name")
            val appliedPackages = bundle.getStringArrayList("appliedPackages") ?: return@setFragmentResultListener
            val templatePackages = bundle.getStringArrayList("templatePackages") ?: return@setFragmentResultListener
            ConfigManager.saveTemplate(
                originalName = info.name,
                name = name,
                isWhitelist = info.isWhitelist,
                appliedPackages = appliedPackages,
                templatePackages = templatePackages.toSet()
            )
            adapter.updateList()
            clearFragmentResultListener("template_settings")
        }

        val args = TemplateSettingsFragmentArgs(info.name, info.isWhitelist)
        navController.navigate(R.id.nav_template_settings, args.toBundle())
    }
}
