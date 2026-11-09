package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val encoder = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
data class JsonConfig(
    var configVersion: Int = BuildConfig.CONFIG_VERSION,
    var detailLog: Boolean = false,
    var maxLogSize: Int = 512,
    var forceMountData: Boolean = true,
    var activityLaunchProtection: Boolean = true,
    val templates: MutableMap<String, Template> = mutableMapOf(),
    val scope: MutableMap<String, AppConfig> = mutableMapOf()
) {
    @Serializable
    data class Template(
        val isWhitelist: Boolean,
        val appList: Set<String>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    data class AppConfig(
        var useWhitelist: Boolean = false,
        var allowSystemApps: Boolean = true,
        var selectedApps: MutableSet<String> = mutableSetOf(),
        var sourceTemplate: String? = null,
        var sourceTemplateApps: MutableSet<String> = mutableSetOf(),

        // Kept only so existing installations can be migrated without losing rules.
        var excludeSystemApps: Boolean? = null,
        var applyTemplates: MutableSet<String> = mutableSetOf(),
        var extraAppList: MutableSet<String> = mutableSetOf()
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<JsonConfig>(json)
    }

    override fun toString() = encoder.encodeToString(this)
}
