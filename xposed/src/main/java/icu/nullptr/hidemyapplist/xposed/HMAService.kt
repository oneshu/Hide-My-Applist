package icu.nullptr.hidemyapplist.xposed

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.util.AtomicFile
import icu.nullptr.hidemyapplist.common.*
import icu.nullptr.hidemyapplist.xposed.hook.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class HMAService(val pms: IPackageManager) : IHMAService.Stub() {

    companion object {
        private const val TAG = "HMA-Service"
        var instance: HMAService? = null
    }

    @Volatile
    var isLoggingAvailable = false

    private lateinit var serviceDataDir: String
    private lateinit var configFile: File
    private lateinit var atomicConfigFile: AtomicFile
    private lateinit var logFile: File
    private lateinit var oldLogFile: File

    private val configLock = Any()
    private val loggerLock = Any()
    private val systemApps = mutableSetOf<String>()
    private val frameworkHooks = mutableSetOf<IFrameworkHook>()

    @Volatile
    var config = JsonConfig().apply { detailLog = true }
        private set

    private val filterCount = AtomicInteger(0)

    fun incrementFilterCount() {
        val count = filterCount.incrementAndGet()
        if (count % 100 == 0) {
            synchronized(configLock) {
                runCatching {
                    File("$serviceDataDir/filter_count").writeText(count.toString())
                }.onFailure {
                    logW(TAG, "Failed to persist filter count", it)
                }
            }
        }
    }

    init {
        searchDataDir()
        instance = this
        loadConfig()
        installHooks()
        logI(TAG, "HMA service initialized")
    }

    private fun searchDataDir() {
        File("/data/system").list()?.forEach {
            if (it.startsWith("hide_my_applist")) {
                if (!this::serviceDataDir.isInitialized) {
                    val relocatedDir = File("/data/misc/$it")
                    File("/data/system/$it").renameTo(relocatedDir)
                    serviceDataDir = relocatedDir.path
                } else {
                    File("/data/system/$it").deleteRecursively()
                }
            }
        }
        File("/data/misc").list()?.forEach {
            if (it.startsWith("hide_my_applist")) {
                if (!this::serviceDataDir.isInitialized) {
                    serviceDataDir = "/data/misc/$it"
                } else if (serviceDataDir != "/data/misc/$it") {
                    File("/data/misc/$it").deleteRecursively()
                }
            }
        }
        if (!this::serviceDataDir.isInitialized) {
            serviceDataDir = "/data/misc/hide_my_applist_" + Utils.generateRandomString(16)
        }

        File("$serviceDataDir/log").mkdirs()
        configFile = File("$serviceDataDir/config.json")
        atomicConfigFile = AtomicFile(configFile)
        logFile = File("$serviceDataDir/log/runtime.log")
        oldLogFile = File("$serviceDataDir/log/old.log")
        logFile.renameTo(oldLogFile)
        logFile.createNewFile()

        isLoggingAvailable = true
        logI(TAG, "Data dir: $serviceDataDir")
    }

    private fun loadConfig() {
        File("$serviceDataDir/filter_count").also {
            runCatching {
                if (it.exists()) filterCount.set(it.readText().toInt())
            }.onFailure { e ->
                logW(TAG, "Failed to load filter count, set to 0", e)
                it.writeText("0")
            }
        }
        if (!configFile.exists()) {
            logI(TAG, "Config file not found")
            return
        }
        val loadedConfig = runCatching {
            val json = atomicConfigFile.readFully().decodeToString()
            JsonConfig.parse(json)
        }.getOrElse {
            logE(TAG, "Failed to parse config.json", it)
            return
        }
        if (loadedConfig.configVersion != BuildConfig.CONFIG_VERSION) {
            logW(TAG, "Config version mismatch, need to reload")
            return
        }
        config = loadedConfig
        logI(TAG, "Config loaded")
    }

    private fun installHooks() {
        Utils.getInstalledApplicationsCompat(pms, 0, 0).mapNotNullTo(systemApps) {
            if (it.flags and ApplicationInfo.FLAG_SYSTEM != 0) it.packageName else null
        }

        frameworkHooks.add(PmsHookTarget36(this))
        frameworkHooks.add(PlatformCompatHook(this))

        frameworkHooks.forEach(IFrameworkHook::load)
        logI(TAG, "Hooks installed")
    }

    fun isHookEnabled(packageName: String) = config.scope.containsKey(packageName)

    fun shouldHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller in Constants.packagesShouldNotHide || query in Constants.packagesShouldNotHide) return false
        if ((caller == Constants.GMS_PACKAGE_NAME || caller == Constants.GSF_PACKAGE_NAME) && query == Constants.APP_PACKAGE_NAME) return false // If apply hide on gms, hma app will crash 😓
        if (caller == query) return false
        val appConfig = config.scope[caller] ?: return false
        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        if (query in appConfig.extraAppList) return !appConfig.useWhitelist
        for (templateName in appConfig.applyTemplates) {
            val template = config.templates[templateName] ?: continue
            if (query in template.appList) return !appConfig.useWhitelist
        }

        return appConfig.useWhitelist
    }

    fun addLog(logEntry: String) {
        synchronized(loggerLock) {
            if (!isLoggingAvailable) return
            if (logFile.length() / 1024 > config.maxLogSize) clearLogs()
            logFile.appendText(logEntry)
        }
    }

    override fun syncConfig(json: String) {
        synchronized(configLock) {
            val newConfig = JsonConfig.parse(json)
            if (newConfig.configVersion != BuildConfig.CONFIG_VERSION) {
                logW(TAG, "Sync config: version mismatch, need reboot")
                return
            }
            writeConfigAtomically(json)
            config = newConfig
            frameworkHooks.forEach(IFrameworkHook::onConfigChanged)
        }
        logD(TAG, "Config synced")
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterCount.get()

    override fun getLogs() = synchronized(loggerLock) {
        logFile.readText()
    }

    override fun clearLogs() {
        synchronized(loggerLock) {
            oldLogFile.delete()
            logFile.renameTo(oldLogFile)
            logFile.createNewFile()
        }
    }

    private fun writeConfigAtomically(json: String) {
        val output = atomicConfigFile.startWrite()
        try {
            output.write(json.toByteArray())
            output.fd.sync()
            atomicConfigFile.finishWrite(output)
        } catch (e: Throwable) {
            atomicConfigFile.failWrite(output)
            throw e
        }
    }
}
