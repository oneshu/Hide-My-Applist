package icu.nullptr.hidemyapplist.xposed

import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private val logExecutor = Executors.newSingleThreadExecutor()

private fun formatLogEntry(
    level: Int,
    tag: String,
    message: String,
    cause: Throwable? = null
) = buildString {
    val levelName = when (level) {
        Log.DEBUG -> "DEBUG"
        Log.INFO -> " INFO"
        Log.WARN -> " WARN"
        Log.ERROR -> "ERROR"
        else -> "?????"
    }
    val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    append("[$levelName] $timestamp ($tag) $message")
    if (!endsWith('\n')) append('\n')
    if (cause != null) append(Log.getStackTraceString(cause))
    if (!endsWith('\n')) append('\n')
}

private fun log(level: Int, tag: String, message: String, cause: Throwable? = null) {
    if (level <= Log.DEBUG && HMAService.instance?.config?.detailLog == false) return
    logExecutor.execute {
        val logEntry = formatLogEntry(level, tag, message, cause)
        HMAService.instance?.addLog(logEntry)
        XposedBridge.log(logEntry)
    }
}

fun logD(tag: String, message: String, cause: Throwable? = null) =
    log(Log.DEBUG, tag, message, cause)

fun logI(tag: String, message: String, cause: Throwable? = null) =
    log(Log.INFO, tag, message, cause)

fun logW(tag: String, message: String, cause: Throwable? = null) =
    log(Log.WARN, tag, message, cause)

fun logE(tag: String, message: String, cause: Throwable? = null) =
    log(Log.ERROR, tag, message, cause)
