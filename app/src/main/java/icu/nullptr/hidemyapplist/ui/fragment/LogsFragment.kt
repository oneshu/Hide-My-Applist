package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentLogsBinding
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.adapter.LogAdapter
import icu.nullptr.hidemyapplist.ui.util.makeToast
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class LogsFragment : Fragment(R.layout.fragment_logs) {

    private val binding by viewBinding<FragmentLogsBinding>()
    private val adapter by lazy { LogAdapter(requireContext()) }
    private var logCache: String? = null

    private val saveSAFLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/x-log")) save@{ uri ->
            if (uri == null) return@save
            if (logCache.isNullOrEmpty()) {
                makeToast(R.string.logs_empty)
                return@save
            }
            hmaApp.contentResolver.openOutputStream(uri).use { output ->
                if (output == null) makeToast(R.string.logs_save_failed)
                else output.write(logCache!!.toByteArray())
            }
            makeToast(R.string.logs_saved)
        }

    private fun updateLogs() {
        lifecycleScope.launch {
            val (cache, logs) = withContext(Dispatchers.IO) {
                val cache = ServiceClient.logs
                cache to cache?.split("\n")?.let { raw ->
                    buildList {
                        val currentEntry = StringBuilder()
                        for (line in raw) {
                            if (line.startsWith('[')) {
                                if (currentEntry.isNotEmpty()) {
                                    val log = LogAdapter.parseLog(currentEntry.toString())
                                    if (log != null) add(log)
                                }
                                currentEntry.clear()
                            }
                            currentEntry.append(line)
                        }
                        if (currentEntry.isNotEmpty()) {
                            val log = LogAdapter.parseLog(currentEntry.toString())
                            if (log != null) add(log)
                        }
                        reverse()
                    }
                }
            }
            logCache = cache
            if (logs == null) {
                binding.serviceOff.visibility = View.VISIBLE
            } else {
                binding.serviceOff.visibility = View.GONE
                adapter.logs = logs
            }
        }
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_refresh -> updateLogs()
            R.id.menu_save -> {
                val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.getDefault()).format(Date())
                saveSAFLauncher.launch("hma_logs_$date.log")
            }
            R.id.menu_delete -> {
                ServiceClient.clearLogs()
                updateLogs()
            }
            R.id.menu_filter_all -> {
                item.isChecked = true
                PrefManager.logFilterLevel = 0
                updateLogs()
            }
            R.id.menu_filter_issues -> {
                item.isChecked = true
                PrefManager.logFilterLevel = 2
                updateLogs()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_logs),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() },
            menuRes = R.menu.menu_logs,
            onMenuOptionSelected = this::onMenuOptionSelected
        )

        with(binding.toolbar.menu) {
            if (PrefManager.logFilterLevel >= 2) {
                findItem(R.id.menu_filter_issues).isChecked = true
            } else {
                findItem(R.id.menu_filter_all).isChecked = true
            }
        }

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        updateLogs()
    }
}
