package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.ui.view.AppItemView

class AppScopeAdapter(
    private val selectedPackageNames: MutableSet<String>,
    packageFilter: ((String) -> Boolean)?,
    private val onSelectionChanged: () -> Unit = {},
) : AppSelectAdapter(packageFilter) {

    private var selectionLocked = false

    private inline var String.isSelected
        get() = this in selectedPackageNames
        set(value) {
            if (value) selectedPackageNames.add(this) else selectedPackageNames.remove(this)
        }

    inner class ViewHolder(view: AppItemView) : AppSelectAdapter.ViewHolder(view) {
        init {
            view.setOnClickListener {
                if (selectionLocked) return@setOnClickListener
                val position = absoluteAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val packageName = visiblePackages[position]
                packageName.isSelected = !packageName.isSelected
                view.isChecked = packageName.isSelected
                onSelectionChanged()
            }
        }

        override fun bind(packageName: String) {
            (itemView as AppItemView).let {
                it.load(packageName)
                it.isChecked = selectionLocked || packageName.isSelected
                it.isCheckboxEnabled = !selectionLocked
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
        if (selectionLocked) return
        selectedPackageNames.clear()
        notifyItemRangeChanged(0, itemCount)
        onSelectionChanged()
    }

    fun setSelectionLocked(locked: Boolean) {
        if (selectionLocked == locked) return
        selectionLocked = locked
        notifyItemRangeChanged(0, itemCount)
    }
}
