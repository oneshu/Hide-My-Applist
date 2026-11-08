package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.CommonUtils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

class PlatformCompatHook(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "PlatformCompatHook"
        private val appDataIsolationEnabled = CommonUtils.isAppDataIsolationEnabled
    }

    private var hook: XC_MethodHook.Unhook? = null

    override fun load() {
        if (!service.config.forceMountData) return
        logI(TAG, "Load hook")
        logI(TAG, "App data isolation enabled: $appDataIsolationEnabled")
        hook = findMethod("com.android.server.compat.PlatformCompat") {
            name == "isChangeEnabled"
        }.hookBefore { param ->
            runCatching {
                val compatChangeId = param.args[0] as Long
                val applicationInfo = param.args[1] as ApplicationInfo
                if (compatChangeId.toInt() != 143937733) return@hookBefore
                val packages = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(applicationInfo.uid)
                } ?: return@hookBefore
                for (packageName in packages) {
                    if (service.isHookEnabled(packageName)) {
                        if (appDataIsolationEnabled) param.result = true
                        logI(TAG, "force mount data: ${applicationInfo.uid} $packageName")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }

    override fun onConfigChanged() {
        if (service.config.forceMountData) {
            if (hook == null) load()
        } else if (hook != null) {
            unload()
        }
    }
}
