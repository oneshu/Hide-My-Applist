package icu.nullptr.hidemyapplist.ui.fragment

import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppManageAdapter
import icu.nullptr.hidemyapplist.ui.util.navController

class AppManageFragment : AppSelectFragment() {

    override val primaryComparator: Comparator<String> =
        Comparator.comparing(ConfigManager::isHideEnabled).reversed()

    override fun getToolbarTitle(): CharSequence = getString(R.string.title_app_manage)

    override val adapter = AppManageAdapter {
        val args = AppSettingsFragmentArgs(it)
        navController.navigate(R.id.nav_app_settings, args.toBundle())
    }
}
