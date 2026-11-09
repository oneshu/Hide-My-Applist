package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.oneshu.applistfilter.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.view.ListItemView
import java.text.Collator
import java.util.*

class TemplateAdapter(
    private val onClickListener: ((ConfigManager.TemplateInfo) -> Unit)?
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    private var list: List<ConfigManager.TemplateInfo> = emptyList()

    init {
        updateList()
    }

    inner class ViewHolder(view: ListItemView) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClickListener?.invoke(list[position])
                }
            }
        }

        fun bind(info: ConfigManager.TemplateInfo) {
            with(itemView as ListItemView) {
                setIcon(
                    if (info.isWhitelist) R.drawable.outline_assignment_24
                    else R.drawable.baseline_assignment_24
                )
                text = info.name
                badgeText =
                    context.getString(if (info.isWhitelist) R.string.whitelist else R.string.blacklist)
                summary = context.getString(
                    if (info.isWhitelist) R.string.template_list_whitelist_summary
                    else R.string.template_list_blacklist_summary,
                    info.appCount
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].name.hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    fun updateList() {
        list = ConfigManager.getTemplateList().apply {
            sortWith { first, second ->
                if (first.isWhitelist != second.isWhitelist) {
                    first.isWhitelist.compareTo(second.isWhitelist)
                } else {
                    Collator.getInstance(Locale.getDefault()).compare(first.name, second.name)
                }
            }
        }
        notifyDataSetChanged()
    }
}
