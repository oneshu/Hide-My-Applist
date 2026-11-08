package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Binder
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import java.util.concurrent.atomic.AtomicReference

class PmsHookTarget36(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "PmsHookTarget36"
    }

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    private var hook: XC_MethodHook.Unhook? = null
    private var archivedPackageHook: XC_MethodHook.Unhook? = null
    private val lastFilteredCaller = AtomicReference<String?>(null)

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, "Load hook")
        hook = findMethod("com.android.server.pm.AppsFilterImpl", findSuper = true) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val snapshot = param.args[0]
                val callingUid = param.args[1] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingPackages = Utils.binderLocalScope {
                    getPackagesForUidMethod.invoke(snapshot, callingUid) as Array<String>?
                } ?: return@hookBefore
                val targetPackage = Utils.getPackageNameFromPackageSettings(param.args[3])
                for (caller in callingPackages) {
                    if (service.shouldHide(caller, targetPackage)) {
                        param.result = true
                        service.incrementFilterCount()
                        val last = lastFilteredCaller.getAndSet(caller)
                        if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                        logD(TAG, "@shouldFilterApplication caller: $callingUid $caller, target: $targetPackage")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }

        // AOSP package visibility bypass fixed in Android 14 QPR2 and present in Android 16.
        archivedPackageHook =
            findMethod("com.android.server.pm.PackageManagerService", findSuper = true) {
                name == "getArchivedPackageInternal"
            }.hookBefore { param ->
                runCatching {
                    val callingUid = Binder.getCallingUid()
                    if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                    val callingPackages = Utils.binderLocalScope {
                        service.pms.getPackagesForUid(callingUid)
                    } ?: return@hookBefore
                    val targetPackage = param.args[0].toString()
                    for (caller in callingPackages) {
                        if (service.shouldHide(caller, targetPackage)) {
                            param.result = null
                            service.incrementFilterCount()
                            val last = lastFilteredCaller.getAndSet(caller)
                            if (last != caller) logI(TAG, "@getArchivedPackageInternal: query from $caller")
                            logD(TAG, "@getArchivedPackageInternal caller: $callingUid $caller, target: $targetPackage")
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
        archivedPackageHook?.unhook()
        archivedPackageHook = null
    }
}
