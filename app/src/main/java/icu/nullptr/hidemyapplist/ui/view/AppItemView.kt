package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import com.oneshu.applistfilter.R
import com.oneshu.applistfilter.databinding.AppItemViewBinding
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding by viewBinding<AppItemViewBinding>(createMethod = CreateMethod.INFLATE)

    var isWhitelist: Boolean?
        get() = if (binding.enabled.visibility == VISIBLE) binding.enabled.tag as? Boolean else null
        set(value) {
            binding.enabled.tag = value
            binding.enabled.visibility = if (value == null) GONE else VISIBLE
            if (value != null) {
                binding.enabled.text = context.getString(
                    if (value) R.string.app_whitelist_mode
                    else R.string.app_blacklist_mode
                )
            }
        }

    var isChecked: Boolean
        get() = binding.checkbox.isChecked
        set(value) {
            binding.checkbox.isChecked = value
        }

    var isCheckboxEnabled: Boolean
        get() = binding.checkbox.isEnabled
        set(value) {
            binding.checkbox.isEnabled = value
        }

    constructor(context: Context, isCheckable: Boolean) : this(context) {
        binding.checkbox.visibility = if (isCheckable) VISIBLE else GONE
    }

    fun load(packageName: String) {
        binding.packageName.text = packageName
        binding.label.text = PackageHelper.loadAppLabel(packageName)
        binding.icon.setImageDrawable(PackageHelper.loadAppIcon(packageName))
    }
}
