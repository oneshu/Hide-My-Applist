package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.*
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oneshu.applistfilter.R
import com.oneshu.applistfilter.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.preference.WorkModePreference
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSettingsViewModel
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppSettingsFragment : Fragment(R.layout.fragment_settings) {

    private data class SettingsSnapshot(
        val hideEnabled: Boolean,
        val config: JsonConfig.AppConfig
    )

    private val binding by viewBinding<FragmentSettingsBinding>()
    private var savedSnapshot: SettingsSnapshot? = null
    private val viewModel by viewModels<AppSettingsViewModel>() {
        val args by navArgs<AppSettingsFragmentArgs>()
        val appConfig = ConfigManager.getAppConfig(args.packageName)
        val state = if (appConfig != null) {
            AppSettingsViewModel.State(args.packageName, true, appConfig.detachedCopy())
        } else {
            AppSettingsViewModel.State(args.packageName, false, JsonConfig.AppConfig())
        }
        AppSettingsViewModel.Factory(state)
    }

    private fun currentSnapshot(): SettingsSnapshot {
        val state = viewModel.state
        val config = state.config.detachedCopy()
        return SettingsSnapshot(state.hideEnabled, config)
    }

    private fun saveConfig() {
        val snapshot = currentSnapshot()
        if (snapshot == savedSnapshot) return
        val state = viewModel.state
        ConfigManager.setAppConfig(
            state.packageName,
            snapshot.config.takeIf { state.hideEnabled }
        )
        savedSnapshot = snapshot
    }

    private fun onBack() {
        saveConfig()
        navController.navigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedSnapshot = currentSnapshot()
    }

    override fun onPause() {
        super.onPause()
        saveConfig()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveConfig()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_settings),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() }
        )

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, AppPreferenceFragment())
                .commit()
        }
    }

    class AppPreferenceDataStore(private val state: AppSettingsViewModel.State) : PreferenceDataStore() {

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "hideEnabled" -> state.hideEnabled
                "allowSystemApps" -> state.config.allowSystemApps
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "hideEnabled" -> state.hideEnabled = value
                "allowSystemApps" -> state.config.allowSystemApps = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class AppPreferenceFragment : PreferenceFragmentCompat() {

        private val parent get() = requireParentFragment() as AppSettingsFragment
        private val state get() = parent.viewModel.state

        private fun updateTemplateSummary() {
            findPreference<Preference>("templateSource")?.apply {
                summary = templateSummary()
            }
        }

        private fun templateName(name: String): String {
            return if (name == ConfigManager.BUILTIN_HIDE_ALL_USERS) {
                getString(R.string.app_template_hide_all_users)
            } else {
                name
            }
        }

        private fun templateSummary(): String {
            val source = state.config.sourceTemplate ?: return getString(R.string.app_templates_none)
            val modified = state.config.selectedApps != state.config.sourceTemplateApps
            return getString(
                if (modified) R.string.app_template_applied_modified
                else R.string.app_template_applied,
                templateName(source)
            )
        }

        private fun appListSummary(): String {
            val selectedCount = state.config.selectedApps.size
            return if (state.config.useWhitelist) {
                when {
                    selectedCount > 0 && state.config.allowSystemApps ->
                        getString(R.string.app_whitelist_selected_system_allowed, selectedCount)
                    selectedCount > 0 ->
                        getString(R.string.app_whitelist_selected_system_hidden, selectedCount)
                    state.config.allowSystemApps ->
                        getString(R.string.app_whitelist_empty_system_allowed)
                    else ->
                        getString(R.string.app_whitelist_empty_system_hidden)
                }
            } else if (selectedCount == 0) {
                getString(R.string.app_blacklist_empty)
            } else {
                getString(R.string.app_blacklist_selected, selectedCount)
            }
        }

        private fun updateEnableSummary() {
            findPreference<SwitchPreference>("hideEnabled")?.apply {
                summary = getString(
                    if (state.hideEnabled) R.string.app_enable_on else R.string.app_enable_off
                )
            }
        }

        private fun updateSystemAppsSummary() {
            findPreference<SwitchPreference>("allowSystemApps")?.summary = getString(
                if (state.config.allowSystemApps) R.string.app_allow_system_on
                else R.string.app_allow_system_off
            )
        }

        private fun updateModeDependentPreferences() {
            findPreference<WorkModePreference>("workMode")?.isWhitelist = state.config.useWhitelist
            findPreference<SwitchPreference>("allowSystemApps")?.isVisible =
                state.config.useWhitelist
            findPreference<Preference>("appList")?.title = getString(
                if (state.config.useWhitelist) R.string.app_whitelist_list_title
                else R.string.app_blacklist_list_title
            )
            findPreference<Preference>("appList")?.summary = appListSummary()
            updateEnableSummary()
            updateSystemAppsSummary()
            updateTemplateSummary()
        }

        private fun applyModeChange(newModeIsWhitelist: Boolean) {
            state.config.useWhitelist = newModeIsWhitelist
            state.config.selectedApps.clear()
            state.config.sourceTemplate = null
            state.config.sourceTemplateApps.clear()
            updateModeDependentPreferences()
        }

        private fun requestModeChange(newModeIsWhitelist: Boolean) {
            val modePreference = findPreference<WorkModePreference>("workMode") ?: return
            if (state.config.useWhitelist == newModeIsWhitelist) return
            if (state.config.selectedApps.isEmpty() && state.config.sourceTemplate == null) {
                applyModeChange(newModeIsWhitelist)
                return
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.work_mode_switch_title)
                .setMessage(R.string.work_mode_switch_message)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    modePreference.isWhitelist = state.config.useWhitelist
                }
                .setPositiveButton(R.string.work_mode_switch_confirm) { _, _ ->
                    applyModeChange(newModeIsWhitelist)
                }
                .setOnCancelListener {
                    modePreference.isWhitelist = state.config.useWhitelist
                }
                .show()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = AppPreferenceDataStore(state)
            setPreferencesFromResource(R.xml.app_settings, rootKey)
            findPreference<Preference>("appInfo")?.let {
                it.isSelectable = false
                it.icon = PackageHelper.loadAppIcon(state.packageName)
                it.title = PackageHelper.loadAppLabel(state.packageName)
                it.summary = PackageHelper.loadPackageInfo(state.packageName).packageName
            }
            findPreference<SwitchPreference>("hideEnabled")?.setOnPreferenceChangeListener { _, _ ->
                view?.post { updateModeDependentPreferences() }
                true
            }
            findPreference<SwitchPreference>("allowSystemApps")?.setOnPreferenceChangeListener {
                _, _ -> view?.post { updateModeDependentPreferences() }
                true
            }
            findPreference<WorkModePreference>("workMode")?.setOnModeChangedListener(
                ::requestModeChange
            )
            findPreference<Preference>("templateSource")?.setOnPreferenceClickListener {
                val templates = ConfigManager.getTemplateList()
                    .filter { it.isWhitelist == state.config.useWhitelist }
                    .mapNotNull { it.name }
                    .sorted()
                val labels = buildList {
                    if (state.config.useWhitelist) {
                        add(getString(R.string.app_template_hide_all_users))
                    }
                    addAll(templates)
                }
                if (labels.isEmpty()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.app_choose_template)
                        .setMessage(R.string.app_no_matching_templates)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return@setOnPreferenceClickListener true
                }
                val sourceName = state.config.sourceTemplate
                val checkedItem = when {
                    sourceName == ConfigManager.BUILTIN_HIDE_ALL_USERS &&
                            state.config.useWhitelist -> 0
                    sourceName != null -> templates.indexOf(sourceName).let {
                        if (it < 0) -1 else it + if (state.config.useWhitelist) 1 else 0
                    }
                    else -> -1
                }
                var selectedIndex = checkedItem
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_template)
                    .setSingleChoiceItems(labels.toTypedArray(), checkedItem) { _, index ->
                        selectedIndex = index
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (selectedIndex < 0) return@setPositiveButton
                        val builtinOffset = if (state.config.useWhitelist) 1 else 0
                        if (state.config.useWhitelist && selectedIndex == 0) {
                            state.config.selectedApps.clear()
                            state.config.sourceTemplate = ConfigManager.BUILTIN_HIDE_ALL_USERS
                            state.config.sourceTemplateApps.clear()
                        } else {
                            val name = templates[selectedIndex - builtinOffset]
                            val packages = ConfigManager.getTemplatePackages(name)
                            state.config.selectedApps = packages.toMutableSet()
                            state.config.sourceTemplate = name
                            state.config.sourceTemplateApps = packages.toMutableSet()
                        }
                        updateModeDependentPreferences()
                    }
                    .show()
                true
            }
            findPreference<Preference>("appList")?.setOnPreferenceClickListener {
                parent.setFragmentResultListener("app_select") { _, bundle ->
                    val selectedPackages =
                        bundle.getStringArrayList("selectedPackages") ?: return@setFragmentResultListener
                    state.config.selectedApps = selectedPackages.toMutableSet()
                    updateModeDependentPreferences()
                    parent.clearFragmentResultListener("app_select")
                }

                val args = ScopeFragmentArgs(
                    filterOnlyEnabled = false,
                    isWhitelist = state.config.useWhitelist,
                    systemAppsAllowed = state.config.allowSystemApps,
                    selectedPackages = state.config.selectedApps.toTypedArray(),
                    allowSaveAsTemplate = true,
                    title = getString(
                        if (state.config.useWhitelist) R.string.app_whitelist_picker_title
                        else R.string.app_blacklist_picker_title
                    )
                )
                parent.navController.navigate(R.id.nav_scope, args.toBundle())
                true
            }
            updateModeDependentPreferences()
        }
    }

    private fun JsonConfig.AppConfig.detachedCopy() = copy(
        selectedApps = selectedApps.toMutableSet(),
        sourceTemplateApps = sourceTemplateApps.toMutableSet(),
        applyTemplates = applyTemplates.toMutableSet(),
        extraAppList = extraAppList.toMutableSet()
    )
}
