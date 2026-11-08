package icu.nullptr.hidemyapplist.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.tsng.hidemyapplist.R

class WorkModePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var blacklistCard: MaterialCardView? = null
    private var whitelistCard: MaterialCardView? = null
    private var modeSummary: TextView? = null
    private var modeChangedListener: ((Boolean) -> Unit)? = null

    var isWhitelist: Boolean = false
        set(value) {
            field = value
            updateCheckedState()
        }

    private fun requestMode(newModeIsWhitelist: Boolean) {
        if (isWhitelist == newModeIsWhitelist) return
        isWhitelist = newModeIsWhitelist
        modeChangedListener?.invoke(newModeIsWhitelist)
    }

    init {
        layoutResource = R.layout.preference_work_mode
        isSelectable = false
        isPersistent = false
    }

    fun setOnModeChangedListener(listener: (Boolean) -> Unit) {
        modeChangedListener = listener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        blacklistCard = holder.itemView.findViewById(R.id.mode_blacklist)
        whitelistCard = holder.itemView.findViewById(R.id.mode_whitelist)
        modeSummary = holder.itemView.findViewById(R.id.work_mode_summary)
        blacklistCard?.setOnClickListener { requestMode(false) }
        whitelistCard?.setOnClickListener { requestMode(true) }
        updateCheckedState()
    }

    private fun updateCheckedState() {
        updateCard(blacklistCard, !isWhitelist)
        updateCard(whitelistCard, isWhitelist)
        updateSummary()
    }

    private fun updateCard(card: MaterialCardView?, selected: Boolean) {
        card ?: return
        val density = card.resources.displayMetrics.density
        card.strokeWidth = ((if (selected) 2 else 1) * density).toInt()
        card.strokeColor = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOutlineVariant
        )
        card.setCardBackgroundColor(
            MaterialColors.getColor(
                card,
                if (selected) com.google.android.material.R.attr.colorPrimaryContainer
                else com.google.android.material.R.attr.colorSurface
            )
        )
    }

    private fun updateSummary() {
        modeSummary?.setText(
            if (isWhitelist) R.string.work_mode_whitelist_summary
            else R.string.work_mode_blacklist_summary
        )
    }
}
