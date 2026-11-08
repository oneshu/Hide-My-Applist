package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import icu.nullptr.hidemyapplist.ui.view.AppItemView

class AppScopeAdapter(
    private val selectedPackageNames: MutableSet<String>,
    packageFilter: ((String) -> Boolean)?,
    private val onSelectionChanged: () -> Unit = {},
) : AppSelectAdapter(packageFilter) {

    private inline var String.isSelected
        get() = this in selectedPackageNames
        set(value) {
            if (value) selectedPackageNames.add(this) else selectedPackageNames.remove(this)
        }

    inner class ViewHolder(view: AppItemView) : AppSelectAdapter.ViewHolder(view) {
        init {
            view.setOnClickListener {
                val packageName = visiblePackages[absoluteAdapterPosition]
                packageName.isSelected = !packageName.isSelected
                view.isChecked = packageName.isSelected
                onSelectionChanged()
            }
        }

        override fun bind(packageName: String) {
            (itemView as AppItemView).let {
                it.load(packageName)
                it.isChecked = packageName.isSelected
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, true)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    fun clearSelection() {
        selectedPackageNames.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }
}
