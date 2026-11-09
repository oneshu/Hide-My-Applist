package icu.nullptr.hidemyapplist.ui.adapter

import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

abstract class AppSelectAdapter(
    private val packageFilter: ((String) -> Boolean)? = null
) : RecyclerView.Adapter<AppSelectAdapter.ViewHolder>(), Filterable {

    abstract class ViewHolder(view: AppItemView) : RecyclerView.ViewHolder(view) {
        abstract fun bind(packageName: String)
    }

    private var showSystemApps = false

    private inner class AppFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            return runBlocking {
                val constraintLowered = constraint.toString().lowercase()
                val matchingPackages = PackageHelper.packageNames.first().filter {
                    if (packageFilter?.invoke(it) == false) return@filter false
                    if (PackageHelper.isSystem(it) != showSystemApps) return@filter false
                    val label = PackageHelper.loadAppLabel(it)
                    val packageInfo = PackageHelper.loadPackageInfo(it)
                    label.lowercase().contains(constraintLowered) || packageInfo.packageName.lowercase().contains(constraintLowered)
                }

                FilterResults().also { it.values = matchingPackages }
            }
        }

        @Suppress("UNCHECKED_CAST", "NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            visiblePackages = results.values as List<String>
            notifyDataSetChanged()
        }
    }

    private val appFilter = AppFilter()

    protected var visiblePackages: List<String> = listOf()

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
    }

    suspend fun categoryCounts(): Pair<Int, Int> {
        val packages = PackageHelper.packageNames.first()
            .filter { packageFilter?.invoke(it) != false }
        return packages.count { !PackageHelper.isSystem(it) } to
                packages.count { PackageHelper.isSystem(it) }
    }

    override fun getItemCount() = visiblePackages.size

    override fun getItemId(position: Int) = visiblePackages[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(visiblePackages[position])

    override fun getFilter(): Filter = appFilter
}
