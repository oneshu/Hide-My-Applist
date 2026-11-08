package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.ResolveInfo
import android.os.Process
import com.github.kyuubiran.ezxhelper.utils.findMethod
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import icu.nullptr.hidemyapplist.xposed.logW
import java.util.concurrent.atomic.AtomicReference

/**
 * Filters activity resolution results for apps whose app list is hidden.
 *
 * The approach is adapted from HMA-OSS (AGPL-3.0), simplified for the
 * Android 16 ComputerEngine implementation used by this project.
 */
class ActivityHook(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "ActivityHook"
        private const val COMPUTER_ENGINE_CLASS = "com.android.server.pm.ComputerEngine"
    }

    private var hook: XC_MethodHook.Unhook? = null
    private val lastFilteredCaller = AtomicReference<String?>(null)

    override fun load() {
        runCatching {
            val method = findMethod(COMPUTER_ENGINE_CLASS) {
                name == "applyPostResolutionFilter"
            }
            hook = XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching {
                        filterResolutionResults(param)
                    }.onFailure {
                        logE(TAG, "Fatal error occurred, disable activity hook", it)
                        unload()
                    }
                }
            })
            logI(TAG, "Load hook")
        }.onFailure {
            logW(TAG, "Activity resolution hook unavailable", it)
        }
    }

    private fun filterResolutionResults(param: XC_MethodHook.MethodHookParam) {
        @Suppress("UNCHECKED_CAST")
        val results = param.result as? List<ResolveInfo> ?: return
        if (results.isEmpty()) return

        val callingUid = param.args.getOrNull(3) as? Int ?: return
        val callingAppId = Utils.getAppId(callingUid)
        if (callingAppId < Process.FIRST_APPLICATION_UID) return

        val callingPackages = Utils.binderLocalScope {
            service.pms.getPackagesForUid(callingUid)
        } ?: return
        val caller = callingPackages.firstOrNull(service::isHookEnabled) ?: return

        val removedTargets = mutableListOf<String>()
        val filtered = results.filter { resolveInfo ->
            val targetPackage = resolveInfo.activityInfo?.packageName
                ?: resolveInfo.resolvePackageName
                ?: return@filter true
            val hide = targetPackage !in callingPackages &&
                    service.shouldHideActivityLaunch(caller, targetPackage)
            if (hide) removedTargets += targetPackage
            !hide
        }
        if (removedTargets.isEmpty()) return

        param.result = filtered
        service.incrementFilterCount(removedTargets.size)
        val last = lastFilteredCaller.getAndSet(caller)
        if (last != caller) {
            logI(TAG, "@applyPostResolutionFilter: query from $caller")
        }
        removedTargets.distinct().forEach {
            logD(TAG, "@applyPostResolutionFilter caller: $callingUid $caller, target: $it")
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }
}
