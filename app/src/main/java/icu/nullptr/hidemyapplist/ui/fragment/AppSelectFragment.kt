package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.adapter.AppSelectAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.launch

abstract class AppSelectFragment : Fragment(R.layout.fragment_app_select) {

    protected val binding by viewBinding<FragmentAppSelectBinding>()

    protected abstract val primaryComparator: Comparator<String>
    protected abstract val adapter: AppSelectAdapter

    private var searchQuery = ""

    protected open fun getToolbarTitle(): CharSequence = getString(R.string.title_app_select)

    protected open fun onBack() {
        navController.navigateUp()
    }

    private fun applyFilter() {
        adapter.filter.filter(searchQuery)
    }

    private fun sortList() {
        lifecycleScope.launch {
            PackageHelper.sortPackages(primaryComparator)
            applyFilter()
        }
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_show_system -> {
                item.isChecked = !item.isChecked
                PrefManager.showSystemApps = item.isChecked
            }
        }
        sortList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getToolbarTitle(),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() },
            menuRes = R.menu.menu_app_list,
            onMenuOptionSelected = this::onMenuOptionSelected
        )

        with(binding.toolbar.menu) {
            val searchView = findItem(R.id.menu_search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    searchQuery = newText
                    applyFilter()
                    return true
                }
            })

            findItem(R.id.menu_show_system).isChecked = PrefManager.showSystemApps
        }
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener {
            PackageHelper.refreshPackages()
        }

        lifecycleScope.launch {
            PackageHelper.isRefreshing
                .flowWithLifecycle(lifecycle)
                .collect {
                    binding.swipeRefresh.isRefreshing = it
                }
        }

        sortList()
    }
}
