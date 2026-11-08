package icu.nullptr.hidemyapplist.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

object PackageHelper {

    class CachedPackage(
        val info: PackageInfo,
        val label: String,
        val icon: Drawable
    )

    private val packageCache = MutableSharedFlow<Map<String, CachedPackage>>(replay = 1)
    private val mutablePackageNames = MutableSharedFlow<List<String>>(replay = 1)
    val packageNames: SharedFlow<List<String>> = mutablePackageNames

    private val mutableRefreshing = MutableSharedFlow<Boolean>(replay = 1)
    val isRefreshing: SharedFlow<Boolean> = mutableRefreshing
    private var refreshJob: Job? = null

    init {
        refreshPackages()
    }

    fun refreshPackages() {
        refreshJob?.cancel()
        refreshJob = hmaApp.globalScope.launch {
            mutableRefreshing.emit(true)
            val loadedPackages = withContext(Dispatchers.IO) {
                val packageManager = hmaApp.packageManager
                val packages = packageManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(0)
                )
                mutableMapOf<String, CachedPackage>().also {
                    for (packageInfo in packages) {
                        if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                        packageInfo.applicationInfo?.let { appInfo ->
                            val label = packageManager.getApplicationLabel(appInfo).toString()
                            val icon = packageManager.getApplicationIcon(appInfo)
                            it[packageInfo.packageName] =
                                CachedPackage(packageInfo, label, icon)
                        }
                    }
                }
            }
            packageCache.emit(loadedPackages)
            mutablePackageNames.emit(loadedPackages.keys.toList())
            mutableRefreshing.emit(false)
        }
    }

    suspend fun sortPackages(primaryComparator: Comparator<String>) {
        val packagesByName = packageCache.first()
        val locale = Locale.getDefault()
        val collator = Collator.getInstance(locale)
        val labelsByName = packagesByName.mapValues { it.value.label.lowercase(locale) }
        val labelComparator = Comparator<String> { first, second ->
            collator.compare(labelsByName[first].orEmpty(), labelsByName[second].orEmpty())
        }
        val sortedPackages = mutablePackageNames.first()
            .filter(packagesByName::containsKey)
            .sortedWith(primaryComparator.then(labelComparator))
        mutablePackageNames.emit(sortedPackages)
    }

    private fun loadCached(packageName: String): CachedPackage? =
        packageCache.replayCache.firstOrNull()?.get(packageName)

    fun loadPackageInfo(packageName: String): PackageInfo = loadCached(packageName)!!.info

    fun loadAppLabel(packageName: String): String = loadCached(packageName)!!.label

    fun loadAppIcon(packageName: String): Drawable = loadCached(packageName)!!.icon

    fun isSystem(packageName: String): Boolean =
        loadCached(packageName)?.info?.applicationInfo?.flags?.and(
            ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        ) != 0
}
