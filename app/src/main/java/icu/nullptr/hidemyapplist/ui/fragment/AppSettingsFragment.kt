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
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentSettingsBinding
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
            AppSettingsViewModel.State(args.packageName, true, appConfig)
        } else {
            AppSettingsViewModel.State(args.packageName, false, JsonConfig.AppConfig())
        }
        AppSettingsViewModel.Factory(state)
    }

    private fun currentSnapshot(): SettingsSnapshot {
        val state = viewModel.state
        val config = state.config.copy(
            applyTemplates = state.config.applyTemplates.toMutableSet(),
            extraAppList = state.config.extraAppList.toMutableSet()
        )
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
                "excludeSystemApps" -> state.config.excludeSystemApps
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "hideEnabled" -> state.hideEnabled = value
                "excludeSystemApps" -> state.config.excludeSystemApps = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class AppPreferenceFragment : PreferenceFragmentCompat() {

        private val parent get() = requireParentFragment() as AppSettingsFragment
        private val state get() = parent.viewModel.state

        private fun updateApplyTemplates() {
            findPreference<Preference>("applyTemplates")?.apply {
                summary = if (state.config.applyTemplates.isEmpty()) {
                    getString(R.string.app_templates_none)
                } else {
                    state.config.applyTemplates.joinToString(
                        getString(R.string.app_template_names_separator)
                    )
                }
            }
        }

        private fun updateExtraAppList() {
            findPreference<Preference>("extraAppList")?.apply {
                summary = if (state.config.extraAppList.isEmpty()) {
                    getString(R.string.app_extra_apps_none)
                } else {
                    getString(R.string.app_extra_apps_selected, state.config.extraAppList.size)
                }
            }
        }

        private fun updateEnableSummary() {
            findPreference<SwitchPreference>("hideEnabled")?.apply {
                summary = getString(
                    if (state.hideEnabled) R.string.app_enable_on else R.string.app_enable_off
                )
            }
        }

        private fun updateModeDependentPreferences() {
            findPreference<WorkModePreference>("workMode")?.isWhitelist = state.config.useWhitelist
            findPreference<SwitchPreference>("excludeSystemApps")?.isVisible = state.config.useWhitelist
            findPreference<PreferenceCategory>("ruleConfigCategory")?.title = getString(
                if (state.config.useWhitelist) R.string.app_rule_config_visible
                else R.string.app_rule_config_hidden
            )
            updateEnableSummary()
            updateApplyTemplates()
            updateExtraAppList()
        }

        private fun applyModeChange(newModeIsWhitelist: Boolean) {
            state.config.useWhitelist = newModeIsWhitelist
            state.config.applyTemplates.clear()
            state.config.extraAppList.clear()
            updateModeDependentPreferences()
        }

        private fun requestModeChange(newModeIsWhitelist: Boolean) {
            val modePreference = findPreference<WorkModePreference>("workMode") ?: return
            if (state.config.useWhitelist == newModeIsWhitelist) return
            if (state.config.applyTemplates.isEmpty() && state.config.extraAppList.isEmpty()) {
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
            findPreference<WorkModePreference>("workMode")?.setOnModeChangedListener(
                ::requestModeChange
            )
            findPreference<Preference>("applyTemplates")?.setOnPreferenceClickListener {
                val templates = ConfigManager.getTemplateList().mapNotNull {
                    if (it.isWhitelist == state.config.useWhitelist) it.name else null
                }.toTypedArray()
                val selectedTemplates = templates.map {
                    state.config.applyTemplates.contains(it)
                }.toBooleanArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_template)
                    .setMultiChoiceItems(templates, selectedTemplates) { _, index, value ->
                        selectedTemplates[index] = value
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        state.config.applyTemplates = templates.mapIndexedNotNullTo(mutableSetOf()) { i, name ->
                            if (selectedTemplates[i]) name else null
                        }
                        updateApplyTemplates()
                    }
                    .show()
                true
            }
            findPreference<Preference>("extraAppList")?.setOnPreferenceClickListener {
                parent.setFragmentResultListener("app_select") { _, bundle ->
                    state.config.extraAppList =
                        bundle.getStringArrayList("selectedPackages")!!.toMutableSet()
                    updateExtraAppList()
                    parent.clearFragmentResultListener("app_select")
                }

                val args = ScopeFragmentArgs(
                    filterOnlyEnabled = false,
                    selectedPackages = state.config.extraAppList.toTypedArray(),
                    title = getString(
                        if (state.config.useWhitelist) R.string.app_extra_apps_visible_title
                        else R.string.app_extra_apps_invisible_title
                    )
                )
                parent.navController.navigate(R.id.nav_scope, args.toBundle())
                true
            }
            updateModeDependentPreferences()
        }
    }
}
