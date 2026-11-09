package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import by.kirich1409.viewbindingdelegate.viewBinding
import com.oneshu.applistfilter.R
import com.oneshu.applistfilter.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.LangList
import rikka.material.app.LocaleDelegate
import rikka.preference.SimpleMenuPreference
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding<FragmentSettingsBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_settings),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() }
        )

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }
    }

    class SettingsPreferenceDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "detailLog" -> ConfigManager.detailLog
                "activityLaunchProtection" -> ConfigManager.activityLaunchProtection
                "hideIcon" -> PrefManager.hideIcon
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "language" -> PrefManager.locale
                "darkTheme" -> PrefManager.darkTheme.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "detailLog" -> ConfigManager.detailLog = value
                "activityLaunchProtection" -> ConfigManager.activityLaunchProtection = value
                "hideIcon" -> PrefManager.hideIcon = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "language" -> PrefManager.locale = value!!
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)

            @Suppress("DEPRECATION")
            findPreference<SimpleMenuPreference>("language")?.let {
                val userLocale = hmaApp.getLocale(PrefManager.locale)
                val entries = buildList {
                    for (lang in LangList.LOCALES) {
                        if (lang == "SYSTEM") {
                            add(getString(rikka.core.R.string.follow_system))
                        } else {
                            val locale = Locale.forLanguageTag(lang)
                            add(
                                HtmlCompat.fromHtml(
                                    locale.getDisplayName(locale),
                                    HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                            )
                        }
                    }
                }
                it.entries = entries.toTypedArray()
                it.entryValues = LangList.LOCALES
                it.summary = if (it.value == "SYSTEM") {
                    getString(rikka.core.R.string.follow_system)
                } else {
                    val locale = Locale.forLanguageTag(it.value)
                    if (!TextUtils.isEmpty(locale.script)) {
                        locale.getDisplayScript(userLocale)
                    } else {
                        locale.getDisplayName(userLocale)
                    }
                }
                it.setOnPreferenceChangeListener { _, newValue ->
                    val localeTag = newValue as String
                    view?.post { applyLocale(localeTag) }
                    true
                }
            }

            findPreference<SimpleMenuPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
                val mode = (newValue as String).toInt()
                if (PrefManager.darkTheme != mode) {
                    view?.post { AppCompatDelegate.setDefaultNightMode(mode) }
                }
                true
            }

        }

        @Suppress("DEPRECATION")
        private fun applyLocale(tag: String) {
            PrefManager.locale = tag
            LocaleDelegate.defaultLocale = hmaApp.getLocale(tag)
            AppCompatDelegate.setApplicationLocales(
                if (tag == "SYSTEM") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
            )
        }
    }
}
