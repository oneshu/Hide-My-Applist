package icu.nullptr.hidemyapplist.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.oneshu.applistfilter.R
import com.oneshu.applistfilter.databinding.LogItemViewBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import java.util.regex.Pattern

class LogAdapter(private val context: Context) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class LogItem(
        val level: String,
        val date: String,
        val tag: String,
        val message: String
    )

    companion object {
        private val LOG_PATTERN =
            Pattern.compile("\\[ ?(.*)] (.*) \\((.*)\\) (.*)", Pattern.DOTALL)
        private const val PACKAGE_PATTERN = "[A-Za-z0-9_.:]+"

        fun parseLog(text: String): LogItem? {
            val matcher = LOG_PATTERN.matcher(text)
            matcher.find()
            val level = matcher.group(1) ?: return null
            if (level == "DEBUG" && PrefManager.logFilterLevel > 0 ||
                level == "INFO" && PrefManager.logFilterLevel > 1 ||
                level == "WARN" && PrefManager.logFilterLevel > 2
            ) return null
            val date = matcher.group(2) ?: return null
            val tag = matcher.group(3) ?: return null
            val message = matcher.group(4) ?: return null
            return LogItem(level, date, tag, message)
        }
    }

    private val appLabelCache = mutableMapOf<String, String>()

    private fun resolveAppLabel(packageName: String): String {
        return appLabelCache.getOrPut(packageName) {
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)
        }
    }

    private fun formatSource(tag: String): String {
        return context.getString(
            when (tag) {
                "HMA-Service" -> R.string.logs_source_system_service
                "HMA-UserService" -> R.string.logs_source_module_service
                "HMA-XposedEntry" -> R.string.logs_source_module_loader
                "PmsHookTarget36" -> R.string.logs_source_app_filter
                "ActivityHook" -> R.string.logs_source_activity_filter
                "PlatformCompatHook" -> R.string.logs_source_data_isolation
                else -> R.string.logs_source_other
            }
        )
    }

    private fun formatMessage(logItem: LogItem): String {
        val line = logItem.message.lineSequence().first().trim()
        val queryFrom = Regex(
            """@(shouldFilterApplication|getArchivedPackageInternal|applyPostResolutionFilter): query from ($PACKAGE_PATTERN)"""
        ).find(line)
        if (queryFrom != null) {
            val caller = resolveAppLabel(queryFrom.groupValues[2])
            return context.getString(
                when (queryFrom.groupValues[1]) {
                    "shouldFilterApplication" -> R.string.logs_event_filtered_query
                    "getArchivedPackageInternal" -> R.string.logs_event_filtered_archive
                    else -> R.string.logs_event_filtered_activity
                },
                caller
            )
        }

        val filterDetail = Regex(
            """@(shouldFilterApplication|getArchivedPackageInternal|applyPostResolutionFilter) caller: \d+ ($PACKAGE_PATTERN), target: ($PACKAGE_PATTERN)"""
        ).find(line)
        if (filterDetail != null) {
            return context.getString(
                R.string.logs_event_filter_detail,
                resolveAppLabel(filterDetail.groupValues[2]),
                resolveAppLabel(filterDetail.groupValues[3])
            )
        }

        return when {
            line.startsWith("Data dir:") -> context.getString(R.string.logs_event_data_ready)
            line == "Config file not found" -> context.getString(R.string.logs_event_config_missing)
            line == "Config loaded" -> context.getString(R.string.logs_event_config_loaded)
            line == "Load hook" -> context.getString(R.string.logs_event_hook_loaded)
            line == "App data isolation enabled: true" ->
                context.getString(R.string.logs_event_data_isolation_enabled)
            line == "App data isolation enabled: false" ->
                context.getString(R.string.logs_event_data_isolation_disabled)
            line == "Hooks installed" -> context.getString(R.string.logs_event_hooks_installed)
            line == "HMA service initialized" -> context.getString(R.string.logs_event_service_started)
            line == "User service started" -> context.getString(R.string.logs_event_user_service_started)
            line == "Send binder to app" -> context.getString(R.string.logs_event_app_connected)
            line == "Register observer" -> context.getString(R.string.logs_event_observer_started)
            line.startsWith("Client uid:") -> context.getString(
                R.string.logs_event_client_connected,
                line.substringAfter(':').trim()
            )
            line == "Hook entry" -> context.getString(R.string.logs_event_module_injected)
            line.startsWith("Got pms:") -> context.getString(R.string.logs_event_pms_connected)
            line.startsWith("Initialize HMAService - Version ") -> context.getString(
                R.string.logs_event_service_initializing,
                line.substringAfterLast(' ')
            )
            line == "Fatal: App signature mismatch" -> context.getString(R.string.logs_event_signature_mismatch)
            line == "Failed to get provider" -> context.getString(R.string.logs_event_provider_failed)
            line == "Failed to send binder to app" -> context.getString(R.string.logs_event_binder_failed)
            line == "Failed to persist filter count" -> context.getString(R.string.logs_event_count_save_failed)
            line == "Failed to load filter count, set to 0" ->
                context.getString(R.string.logs_event_count_load_failed)
            line == "Failed to parse config.json" -> context.getString(R.string.logs_event_config_parse_failed)
            line == "Config version mismatch, need to reload" ->
                context.getString(R.string.logs_event_config_version_mismatch)
            line == "Sync config: version mismatch, need reboot" ->
                context.getString(R.string.logs_event_sync_needs_restart)
            line == "Config synced" -> context.getString(R.string.logs_event_config_synced)
            line.startsWith("Fatal error occurred") || line == "System service crashed" ||
                line == "onUidActive" -> context.getString(R.string.logs_event_hook_error)
            line.startsWith("force mount data:") -> context.getString(R.string.logs_event_force_mount)
            else -> line
        }
    }

    var logs = listOf<LogItem>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: LogItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: LogItem) {
            binding.date.text = logItem.date
            binding.tag.text = formatSource(logItem.tag)
            binding.message.text = formatMessage(logItem)
            binding.rawDetails.text = logItem.message.trim()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = logs.size

    override fun getItemId(position: Int) = logs[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(logs[position])
}
