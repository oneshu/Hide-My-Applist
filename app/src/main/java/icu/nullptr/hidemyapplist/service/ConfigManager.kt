package icu.nullptr.hidemyapplist.service

import android.util.Log
import android.util.AtomicFile
import com.oneshu.applistfilter.R
import icu.nullptr.hidemyapplist.common.BuildConfig
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.ui.util.makeToast
import java.io.File

object ConfigManager {

    const val BUILTIN_HIDE_ALL_USERS = "@builtin:hide-all-users"

    data class TemplateInfo(
        val name: String?,
        val isWhitelist: Boolean,
        val appCount: Int = 0
    )

    private const val TAG = "ConfigManager"
    private const val AGGREGATED_RULES_VERSION = 91
    private lateinit var config: JsonConfig
    val configFile = File("${hmaApp.filesDir.absolutePath}/config.json")
    private val atomicConfigFile by lazy { AtomicFile(configFile) }

    fun init() {
        if (!configFile.exists()) {
            writeConfigFile(JsonConfig().toString())
        }
        runCatching {
            config = JsonConfig.parse(atomicConfigFile.readFully().decodeToString())
            val configVersion = config.configVersion
            if (configVersion < 65) throw RuntimeException("Config version too old")
            if (configVersion < AGGREGATED_RULES_VERSION) migrateAppRules()
            config.configVersion = BuildConfig.CONFIG_VERSION
            saveConfig()
        }.onFailure {
            makeToast(R.string.config_damaged)
            throw RuntimeException("Config file too old or damaged", it)
        }
    }

    private fun migrateAppRules() {
        config.scope.values.forEach { appConfig ->
            val templatePackages = appConfig.applyTemplates.flatMapTo(mutableSetOf()) { name ->
                config.templates[name]?.appList.orEmpty()
            }
            appConfig.selectedApps = (
                appConfig.extraAppList + templatePackages
            ).toMutableSet()
            if (appConfig.applyTemplates.size == 1) {
                appConfig.sourceTemplate = appConfig.applyTemplates.single()
                appConfig.sourceTemplateApps = templatePackages
            }
            appConfig.excludeSystemApps?.let { appConfig.allowSystemApps = it }
            appConfig.excludeSystemApps = null
            appConfig.applyTemplates.clear()
            appConfig.extraAppList.clear()
        }
    }

    private fun saveConfig() {
        val text = config.toString()
        writeConfigFile(text)
        val serviceText = if (PrefManager.globalHidingEnabled) {
            text
        } else {
            config.copy(scope = mutableMapOf()).toString()
        }
        ServiceClient.syncConfig(serviceText)
    }

    fun resyncServiceConfig() = saveConfig()

    private fun writeConfigFile(text: String) {
        val output = atomicConfigFile.startWrite()
        try {
            output.write(text.toByteArray())
            output.fd.sync()
            atomicConfigFile.finishWrite(output)
        } catch (e: Throwable) {
            atomicConfigFile.failWrite(output)
            throw e
        }
    }

    var detailLog: Boolean
        get() = config.detailLog
        set(value) {
            config.detailLog = value
            saveConfig()
        }

    var forceMountData: Boolean
        get() = config.forceMountData
        set(value) {
            config.forceMountData = value
            saveConfig()
        }

    var activityLaunchProtection: Boolean
        get() = config.activityLaunchProtection
        set(value) {
            config.activityLaunchProtection = value
            saveConfig()
        }

    fun hasTemplate(name: String?): Boolean {
        return name == BUILTIN_HIDE_ALL_USERS || config.templates.containsKey(name)
    }

    fun getTemplateList(): MutableList<TemplateInfo> {
        return config.templates.mapTo(mutableListOf()) { (name, template) ->
            TemplateInfo(
                name = name,
                isWhitelist = template.isWhitelist,
                appCount = template.appList.size
            )
        }
    }

    fun getTemplatePackages(name: String): ArrayList<String> {
        return ArrayList(config.templates[name]?.appList ?: emptyList())
    }

    fun getEnabledAppCount(): Int = config.scope.size

    fun saveTemplate(
        originalName: String?,
        name: String?,
        isWhitelist: Boolean,
        templatePackages: Set<String>
    ) {
        Log.d(TAG, "saveTemplate: $originalName -> $name, packages = $templatePackages")
        if (name == null) {
            originalName ?: return
            config.scope.values.forEach { appConfig ->
                if (appConfig.sourceTemplate == originalName) {
                    appConfig.sourceTemplate = null
                    appConfig.sourceTemplateApps.clear()
                }
            }
            config.templates.remove(originalName)
            saveConfig()
            return
        }

        if (originalName != null && originalName != name) {
            config.templates.remove(originalName)
            config.scope.values.forEach { appConfig ->
                if (appConfig.sourceTemplate == originalName) {
                    appConfig.sourceTemplate = name
                }
            }
        }
        config.templates[name] = JsonConfig.Template(isWhitelist, templatePackages)
        saveConfig()
    }

    fun isHideEnabled(packageName: String): Boolean {
        return config.scope.containsKey(packageName)
    }

    fun getAppConfig(packageName: String): JsonConfig.AppConfig? {
        return config.scope[packageName]
    }

    fun setAppConfig(packageName: String, appConfig: JsonConfig.AppConfig?) {
        if (appConfig == null) config.scope.remove(packageName)
        else config.scope[packageName] = appConfig
        saveConfig()
    }
}
