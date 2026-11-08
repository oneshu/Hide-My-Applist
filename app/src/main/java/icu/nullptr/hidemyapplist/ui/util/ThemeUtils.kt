package icu.nullptr.hidemyapplist.ui.util

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.tsng.hidemyapplist.R

object ThemeUtils {

    @get:StyleRes
    val colorThemeStyleRes: Int
        get() = R.style.ThemeOverlay_MaterialBlue

    @get:StyleRes
    val nightThemeStyleRes: Int
        get() = R.style.ThemeOverlay

    /**
     * Retrieve a color from the current [android.content.res.Resources.Theme].
     */
    @ColorInt
    fun Context.themeColor(
        @AttrRes themeAttrId: Int
    ): Int {
        val style = obtainStyledAttributes(intArrayOf(themeAttrId))
        val color = style.getColor(0, Color.MAGENTA)
        style.recycle()
        return color
    }

    @ColorInt
    fun Fragment.themeColor(
        @AttrRes themeAttrId: Int
    ) = requireContext().themeColor(themeAttrId)

    @ColorInt
    fun Fragment.getColor(
        @ColorRes colorId: Int
    ) = requireContext().getColor(colorId)
}
