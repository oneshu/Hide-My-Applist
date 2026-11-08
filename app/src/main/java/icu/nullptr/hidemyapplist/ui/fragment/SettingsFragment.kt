package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.common.CommonUtils
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.makeToast
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.LangList
import icu.nullptr.hidemyapplist.util.SuUtils
import rikka.material.app.LocaleDelegate
import rikka.preference.SimpleMenuPreference
import java.util.*

class SettingsFragment : Fragment(R.layout.fragment_settings), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val binding by viewBinding<FragmentSettingsBinding>()
    private var subpageTitle: String? = null
    private val backStackChangedListener = FragmentManager.OnBackStackChangedListener {
        if (view != null && isAdded) updateToolbar()
    }

    private companion object {
        const val ARG_SUBPAGE_TITLE = "subpage_title"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        childFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (childFragmentManager.popBackStackImmediate()) return
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        )
        updateToolbar()

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        subpageTitle = pref.title?.toString()
        val fragment = childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, pref.fragment!!)
        fragment.arguments = Bundle().apply {
            pref.extras?.let { putAll(it) }
            putCharSequence(ARG_SUBPAGE_TITLE, pref.title)
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(backStackChangedListener)
        super.onDestroyView()
    }

    private fun updateToolbar() {
        val hasSubpage = childFragmentManager.backStackEntryCount > 0
        if (hasSubpage) {
            val currentTitle = childFragmentManager.fragments
                .lastOrNull()
                ?.arguments
                ?.getCharSequence(ARG_SUBPAGE_TITLE)
                ?.toString()
            setupToolbar(
                toolbar = binding.toolbar,
                title = currentTitle ?: subpageTitle ?: getString(R.string.title_settings),
                navigationIcon = R.drawable.baseline_arrow_back_24,
                navigationOnClick = { childFragmentManager.popBackStack() }
            )
        } else {
            setupToolbar(binding.toolbar, getString(R.string.title_settings))
            binding.toolbar.setNavigationIcon(null)
            binding.toolbar.setNavigationOnClickListener(null)
        }
    }

    class SettingsPreferenceDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                "detailLog" -> ConfigManager.detailLog
                "hideIcon" -> PrefManager.hideIcon
                "appDataIsolation" -> CommonUtils.isAppDataIsolationEnabled
                "voldAppDataIsolation" -> CommonUtils.isVoldAppDataIsolationEnabled
                "forceMountData" -> ConfigManager.forceMountData
                "disableUpdate" -> PrefManager.disableUpdate
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "language" -> PrefManager.locale
                "themeColor" -> PrefManager.themeColor
                "darkTheme" -> PrefManager.darkTheme.toString()
                "maxLogSize" -> ConfigManager.maxLogSize.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                "detailLog" -> ConfigManager.detailLog = value
                "forceMountData" -> ConfigManager.forceMountData = value
                "hideIcon" -> PrefManager.hideIcon = value
                "appDataIsolation" -> Unit
                "voldAppDataIsolation" -> Unit
                "disableUpdate" -> PrefManager.disableUpdate = value
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "language" -> PrefManager.locale = value!!
                "themeColor" -> PrefManager.themeColor = value!!
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                "maxLogSize" -> ConfigManager.maxLogSize = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class DataIsolationPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings_data_isolation, rootKey)

            findPreference<SwitchPreference>("appDataIsolation")?.let {
                it.setOnPreferenceChangeListener { _, newValue ->
                    handleIsolationChange(
                        preference = it,
                        enabled = newValue as Boolean,
                        property = Constants.ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY,
                        checker = CommonUtils::isAppDataIsolationEnabled
                    )
                    false
                }
            }

            findPreference<SwitchPreference>("voldAppDataIsolation")?.let {
                it.setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    if (enabled) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.settings_warning)
                            .setMessage(R.string.settings_vold_warning)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                handleIsolationChange(
                                    preference = it,
                                    enabled = true,
                                    property = Constants.ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY,
                                    checker = CommonUtils::isVoldAppDataIsolationEnabled
                                )
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                it.isChecked = false
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        handleIsolationChange(
                            preference = it,
                            enabled = false,
                            property = Constants.ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY,
                            checker = CommonUtils::isVoldAppDataIsolationEnabled
                        )
                    }
                    false
                }
            }
        }

        private fun handleIsolationChange(preference: SwitchPreference, enabled: Boolean, property: String, checker: () -> Boolean) {
            val value = if (enabled) 1 else 0
            val result = SuUtils.execPrivileged("setprop $property $value")
            if (result) makeToast(R.string.settings_need_reboot)
            else makeToast(R.string.settings_permission_denied)
            preference.isChecked = checker()
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        private fun Boolean.enabledString(): String {
            return if (this) getString(R.string.enabled)
            else getString(R.string.disabled)
        }

        private fun recreateAfterPreferenceChange() {
            view?.post { activity?.recreate() }
        }

        private fun configureDataIsolation() {
            findPreference<Preference>("dataIsolation")?.let {
                it.summary = getString(
                    R.string.settings_data_isolation_summary,
                    CommonUtils.isAppDataIsolationEnabled.enabledString(),
                    CommonUtils.isVoldAppDataIsolationEnabled.enabledString(),
                    ConfigManager.forceMountData.enabledString()
                )
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)

            @Suppress("DEPRECATION")
            findPreference<SimpleMenuPreference>("language")?.let {
                val userLocale = hmaApp.getLocale(PrefManager.locale)
                val entries = buildList {
                    for (lang in LangList.LOCALES) {
                        if (lang == "SYSTEM") add(getString(rikka.core.R.string.follow_system))
                        else {
                            val locale = Locale.forLanguageTag(lang)
                            add(HtmlCompat.fromHtml(locale.getDisplayName(locale), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        }
                    }
                }
                it.entries = entries.toTypedArray()
                it.entryValues = LangList.LOCALES
                if (it.value == "SYSTEM") {
                    it.summary = getString(rikka.core.R.string.follow_system)
                } else {
                    val locale = Locale.forLanguageTag(it.value)
                    it.summary = if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(userLocale) else locale.getDisplayName(userLocale)
                }
                it.setOnPreferenceChangeListener { _, newValue ->
                    val localeTag = newValue as String
                    view?.post {
                        val locale = hmaApp.getLocale(localeTag)
                        val config = resources.configuration
                        config.setLocale(locale)
                        LocaleDelegate.defaultLocale = locale
                        hmaApp.resources.updateConfiguration(config, resources.displayMetrics)
                        activity?.recreate()
                    }
                    true
                }
            }

            findPreference<SwitchPreference>("followSystemAccent")?.setOnPreferenceChangeListener { _, _ ->
                recreateAfterPreferenceChange()
                true
            }

            findPreference<SimpleMenuPreference>("themeColor")?.setOnPreferenceChangeListener { _, _ ->
                recreateAfterPreferenceChange()
                true
            }

            findPreference<SimpleMenuPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
                val newMode = (newValue as String).toInt()
                if (PrefManager.darkTheme != newMode) {
                    view?.post { AppCompatDelegate.setDefaultNightMode(newMode) }
                }
                true
            }

            findPreference<SwitchPreference>("blackDarkTheme")?.setOnPreferenceChangeListener { _, _ ->
                recreateAfterPreferenceChange()
                true
            }

            configureDataIsolation()

            findPreference<Preference>("stopSystemService")?.setOnPreferenceClickListener {
                if (ServiceClient.serviceVersion != 0) {
                    val cleanRuntime = booleanArrayOf(false)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_stop_system_service)
                        .setMessage(R.string.settings_stop_system_service_summary)
                        .setMultiChoiceItems(
                            arrayOf(getString(R.string.settings_is_clean_env)),
                            cleanRuntime
                        ) { _, which, checked ->
                            cleanRuntime[which] = checked
                        }
                        .setPositiveButton(R.string.stop) { _, _ ->
                            ServiceClient.stopService(cleanRuntime[0])
                            makeToast(R.string.settings_stop_system_service_toast)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else makeToast(R.string.home_xposed_service_off)
                true
            }

            findPreference<Preference>("forceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_force_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result = SuUtils.execPrivileged("rm -rf /data/misc/hide_my_applist*")
                        if (result) makeToast(R.string.settings_force_clean_env_toast_successful)
                        else makeToast(R.string.settings_permission_denied)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            configureDataIsolation()
        }
    }
}
