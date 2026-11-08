package icu.nullptr.hidemyapplist.service

import android.util.Log
import android.util.AtomicFile
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.common.BuildConfig
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.ui.util.makeToast
import java.io.File

object ConfigManager {

    data class TemplateInfo(
        val name: String?,
        val isWhiteList: Boolean,
        val appCount: Int = 0,
        val appliedCount: Int = 0
    )

    private const val TAG = "ConfigManager"
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
            config.configVersion = BuildConfig.CONFIG_VERSION
            saveConfig()
        }.onFailure {
            makeToast(R.string.config_damaged)
            throw RuntimeException("Config file too old or damaged", it)
        }
    }

    private fun saveConfig() {
        val text = config.toString()
        writeConfigFile(text)
        ServiceClient.syncConfig(text)
    }

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

    var maxLogSize: Int
        get() = config.maxLogSize
        set(value) {
            config.maxLogSize = value
            saveConfig()
        }

    var forceMountData: Boolean
        get() = config.forceMountData
        set(value) {
            config.forceMountData = value
            saveConfig()
        }

    fun importConfig(json: String) {
        config = JsonConfig.parse(json)
        config.configVersion = BuildConfig.CONFIG_VERSION
        saveConfig()
    }

    fun hasTemplate(name: String?): Boolean {
        return config.templates.containsKey(name)
    }

    fun getTemplateList(): MutableList<TemplateInfo> {
        return config.templates.mapTo(mutableListOf()) { (name, template) ->
            TemplateInfo(
                name = name,
                isWhiteList = template.isWhitelist,
                appCount = template.appList.size,
                appliedCount = config.scope.count { (_, appConfig) ->
                    appConfig.applyTemplates.contains(name)
                }
            )
        }
    }

    fun getTemplateAppliedAppList(name: String): ArrayList<String> {
        return config.scope.mapNotNullTo(ArrayList()) {
            if (it.value.applyTemplates.contains(name)) it.key else null
        }
    }

    fun getTemplateTargetAppList(name: String): ArrayList<String> {
        return ArrayList(config.templates[name]?.appList ?: emptyList())
    }

    fun getEnabledAppCount(): Int = config.scope.size

    fun saveTemplate(
        originalName: String?,
        name: String?,
        isWhiteList: Boolean,
        appliedList: List<String>,
        targetList: Set<String>
    ) {
        Log.d(TAG, "saveTemplate: $originalName -> $name, apps = $targetList")
        if (name == null) {
            originalName ?: return
            config.scope.forEach { (_, appInfo) -> appInfo.applyTemplates.remove(originalName) }
            config.templates.remove(originalName)
            saveConfig()
            return
        }

        if (originalName != null && originalName != name) {
            config.templates.remove(originalName)
            config.scope.values.forEach { it.applyTemplates.remove(originalName) }
        }
        config.templates[name] = JsonConfig.Template(isWhiteList, targetList)
        config.scope.forEach { (app, appInfo) ->
            if (app in appliedList) appInfo.applyTemplates.add(name)
            else appInfo.applyTemplates.remove(name)
        }
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
