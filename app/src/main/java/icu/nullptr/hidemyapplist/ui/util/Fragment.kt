package icu.nullptr.hidemyapplist.ui.util

import android.view.MenuItem
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment

val Fragment.navController get() = NavHostFragment.findNavController(this)

fun Fragment.setupToolbar(
    toolbar: Toolbar,
    title: CharSequence,
    @DrawableRes navigationIcon: Int? = null,
    navigationOnClick: View.OnClickListener? = null,
    @MenuRes menuRes: Int? = null,
    onMenuOptionSelected: ((MenuItem) -> Unit)? = null
) {
    navigationOnClick?.let { toolbar.setNavigationOnClickListener(it) }
    navigationIcon?.let { toolbar.setNavigationIcon(navigationIcon) }
    toolbar.title = title
    toolbar.tooltipText = title
    toolbar.menu.clear()
    if (menuRes != null) {
        toolbar.inflateMenu(menuRes)
        toolbar.setOnMenuItemClickListener { item ->
            val handler = onMenuOptionSelected ?: return@setOnMenuItemClickListener false
            handler(item)
            true
        }
    } else {
        toolbar.setOnMenuItemClickListener(null)
    }
}
