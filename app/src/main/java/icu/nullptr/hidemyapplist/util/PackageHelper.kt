package icu.nullptr.hidemyapplist.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

interface PackageManagerDelegate {
    fun getInstalledPackages(flags: Int): List<PackageInfo>
    fun getApplicationLabel(info: ApplicationInfo): CharSequence
}

class DefaultPackageManagerDelegate(private val packageManager: PackageManager) : PackageManagerDelegate {
    override fun getInstalledPackages(flags: Int): List<PackageInfo> {
        return packageManager.getInstalledPackages(flags)
    }

    override fun getApplicationLabel(info: ApplicationInfo): CharSequence {
        return packageManager.getApplicationLabel(info)
    }
}

object PackageHelper {

    class PackageCache(
        val info: PackageInfo,
        val label: String,
        val icon: Bitmap
    )

    private lateinit var packageManagerDelegate: PackageManagerDelegate

    private val packageCache = MutableSharedFlow<Map<String, PackageCache>>(replay = 1)
    private val mAppList = MutableSharedFlow<List<String>>(replay = 1)
    val appList: SharedFlow<List<String>> = mAppList

    private val mRefreshing = MutableSharedFlow<Boolean>(replay = 1)
    val isRefreshing: SharedFlow<Boolean> = mRefreshing
    private var refreshJob: Job? = null

    init {
        packageManagerDelegate = DefaultPackageManagerDelegate(hmaApp.packageManager)
        invalidateCache()
    }

    fun setPackageManagerDelegate(delegate: PackageManagerDelegate) {
        packageManagerDelegate = delegate
    }

    fun invalidateCache() {
        refreshJob?.cancel()
        refreshJob = hmaApp.globalScope.launch {
            mRefreshing.emit(true)
            val cache = withContext(Dispatchers.IO) {
                val packages = packageManagerDelegate.getInstalledPackages(0)
                mutableMapOf<String, PackageCache>().also {
                    for (packageInfo in packages) {
                        if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                        packageInfo.applicationInfo?.let { appInfo ->
                            val label = packageManagerDelegate.getApplicationLabel(appInfo).toString()
                            val icon = hmaApp.appIconLoader.loadIcon(appInfo)
                            it[packageInfo.packageName] = PackageCache(packageInfo, label, icon)
                        }
                    }
                }
            }
            packageCache.emit(cache)
            mAppList.emit(cache.keys.toList())
            mRefreshing.emit(false)
        }
    }

    suspend fun sortList(firstComparator: Comparator<String>) {
        val cache = packageCache.first()
        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale)
        val labels = cache.mapValues { it.value.label.lowercase(locale) }
        val packageNames = cache.mapValues { it.key.lowercase(locale) }
        val installTimes = cache.mapValues { it.value.info.firstInstallTime }
        val updateTimes = cache.mapValues { it.value.info.lastUpdateTime }
        var comparator = when (PrefManager.appFilter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> Comparator<String> { a, b ->
                collator.compare(labels[a].orEmpty(), labels[b].orEmpty())
            }
            PrefManager.SortMethod.BY_PACKAGE_NAME -> Comparator<String> { a, b ->
                collator.compare(packageNames[a].orEmpty(), packageNames[b].orEmpty())
            }
            PrefManager.SortMethod.BY_INSTALL_TIME -> Comparator<String> { a, b ->
                installTimes.getValue(b).compareTo(installTimes.getValue(a))
            }
            PrefManager.SortMethod.BY_UPDATE_TIME -> Comparator<String> { a, b ->
                updateTimes.getValue(b).compareTo(updateTimes.getValue(a))
            }
        }
        if (PrefManager.appFilter_reverseOrder) comparator = comparator.reversed()
        val list = mAppList.first()
            .filter(cache::containsKey)
            .sortedWith(firstComparator.then(comparator))
        mAppList.emit(list)
    }

    private fun loadCached(packageName: String): PackageCache? =
        packageCache.replayCache.firstOrNull()?.get(packageName)

    fun loadPackageInfo(packageName: String): PackageInfo = loadCached(packageName)!!.info

    fun loadAppLabel(packageName: String): String = loadCached(packageName)!!.label

    fun loadAppIcon(packageName: String): Bitmap = loadCached(packageName)!!.icon

    fun isSystem(packageName: String): Boolean =
        loadCached(packageName)?.info?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
}
