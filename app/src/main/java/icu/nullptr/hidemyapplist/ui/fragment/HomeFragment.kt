package icu.nullptr.hidemyapplist.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentHomeBinding
import icu.nullptr.hidemyapplist.common.CommonUtils
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.getColor
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import icu.nullptr.hidemyapplist.ui.util.makeToast
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.SuUtils

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding<FragmentHomeBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.app_name),
            menuRes = R.menu.menu_home,
            onMenuOptionSelected = { item ->
                if (item.itemId == R.id.menu_settings) {
                    navController.navigate(R.id.nav_settings)
                }
            }
        )

        binding.statusCard.setOnClickListener {
            navController.navigate(R.id.nav_logs)
        }
        binding.templateManage.setOnClickListener {
            navController.navigate(R.id.nav_template_manage)
        }
        binding.appManage.setOnClickListener {
            navController.navigate(R.id.nav_app_manage)
        }
        binding.globalHideRow.setOnClickListener {
            binding.globalHideSwitch.isChecked = !binding.globalHideSwitch.isChecked
        }
        binding.dataIsolationRow.setOnClickListener {
            binding.dataIsolationSwitch.isChecked = !binding.dataIsolationSwitch.isChecked
        }
        binding.forceMountRow.setOnClickListener {
            binding.forceMountSwitch.isChecked = !binding.forceMountSwitch.isChecked
        }
        binding.globalHideSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (PrefManager.globalHidingEnabled != isChecked) {
                PrefManager.globalHidingEnabled = isChecked
                ConfigManager.resyncServiceConfig()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateConfigSummary()
        updateFunctionSummary()
        val serviceVersion = ServiceClient.serviceVersion
        val color = when {
            !hmaApp.isHooked -> getColor(R.color.gray)
            serviceVersion == 0 -> getColor(R.color.invalid)
            else -> themeColor(android.R.attr.colorPrimary)
        }
        binding.moduleStatusIcon.imageTintList = ColorStateList.valueOf(color)
        if (hmaApp.isHooked) {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_done_all_24)
            val versionNameSimple = BuildConfig.VERSION_NAME.substringBefore(".r")
            binding.moduleStatus.text = getString(R.string.home_xposed_activated, versionNameSimple, BuildConfig.VERSION_CODE)
        } else {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_extension_off_24)
            binding.moduleStatus.setText(R.string.home_xposed_not_activated)
        }
        if (serviceVersion != 0) {
            if (serviceVersion < icu.nullptr.hidemyapplist.common.BuildConfig.SERVICE_VERSION) {
                binding.serviceStatus.text = getString(R.string.home_xposed_service_old)
            } else {
                binding.serviceStatus.text = getString(R.string.home_xposed_service_on, serviceVersion)
            }
            binding.filterCount.visibility = View.VISIBLE
            binding.filterCount.text = getString(R.string.home_xposed_filter_count, ServiceClient.filterCount)
        } else {
            binding.serviceStatus.setText(R.string.home_xposed_service_off)
            binding.filterCount.visibility = View.GONE
        }
    }

    private fun updateConfigSummary() {
        val templateCount = ConfigManager.getTemplateList().size
        binding.templateManage.summary = if (templateCount == 0) {
            getString(R.string.home_no_templates)
        } else {
            getString(R.string.home_template_count, templateCount)
        }
        binding.appManage.summary = getString(
            R.string.home_app_count,
            ConfigManager.getEnabledAppCount()
        )
    }

    private fun updateFunctionSummary() {
        binding.globalHideSwitch.setOnCheckedChangeListener(null)
        binding.globalHideSwitch.isChecked = PrefManager.globalHidingEnabled
        binding.globalHideSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (PrefManager.globalHidingEnabled != isChecked) {
                PrefManager.globalHidingEnabled = isChecked
                ConfigManager.resyncServiceConfig()
            }
        }
        val dataIsolationEnabled = CommonUtils.isAppDataIsolationEnabled
        binding.dataIsolationSwitch.setOnCheckedChangeListener(null)
        binding.dataIsolationSwitch.isChecked = dataIsolationEnabled
        binding.dataIsolationSummary.setText(
            if (dataIsolationEnabled) {
                R.string.home_data_isolation_on_summary
            } else {
                R.string.home_data_isolation_off_summary
            }
        )
        binding.dataIsolationSwitch.setOnCheckedChangeListener { _, isChecked ->
            val property = Constants.ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY
            val result = SuUtils.execPrivileged("setprop $property ${if (isChecked) 1 else 0}")
            makeToast(if (result) R.string.settings_need_reboot else R.string.settings_permission_denied)
            updateFunctionSummary()
        }
        binding.forceMountRow.visibility = if (dataIsolationEnabled) View.VISIBLE else View.GONE
        binding.forceMountSwitch.setOnCheckedChangeListener(null)
        binding.forceMountSwitch.isChecked = ConfigManager.forceMountData
        binding.forceMountSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ConfigManager.forceMountData != isChecked) {
                ConfigManager.forceMountData = isChecked
            }
        }
    }
}
