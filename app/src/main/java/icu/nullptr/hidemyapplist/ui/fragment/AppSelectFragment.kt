package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.tabs.TabLayout
import com.oneshu.applistfilter.R
import com.oneshu.applistfilter.databinding.FragmentAppSelectBinding
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

    protected open fun onAppCategoryChanged(showSystemApps: Boolean) = Unit

    private fun applyFilter() {
        adapter.filter.filter(searchQuery)
    }

    protected fun sortList() {
        lifecycleScope.launch {
            PackageHelper.sortPackages(primaryComparator)
            applyFilter()
            updateTabCounts()
        }
    }

    private suspend fun updateTabCounts() {
        val (userCount, systemCount) = adapter.categoryCounts()
        binding.categoryTabs.getTabAt(0)?.text =
            getString(R.string.app_tab_user, userCount)
        binding.categoryTabs.getTabAt(1)?.text =
            getString(R.string.app_tab_system, systemCount)
    }

    private fun selectCategory(showSystemApps: Boolean) {
        adapter.setShowSystemApps(showSystemApps)
        onAppCategoryChanged(showSystemApps)
        applyFilter()
    }

    private fun setupCategories() {
        with(binding.categoryTabs) {
            removeAllTabs()
            addTab(newTab().setText(getString(R.string.app_tab_user, 0)))
            addTab(newTab().setText(getString(R.string.app_tab_system, 0)))
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    selectCategory(tab.position == 1)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getToolbarTitle(),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() },
            menuRes = R.menu.menu_app_list,
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

        }
        setupCategories()
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
                    if (!it) sortList()
                }
        }

        sortList()
    }
}
