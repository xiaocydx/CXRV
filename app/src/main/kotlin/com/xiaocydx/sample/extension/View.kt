@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.sample

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.viewpager2.widget.ViewPager2

/**
 * [TypedValue.complexToDimensionPixelSize]的舍入逻辑，
 * 用于确保[dp]转换的px值，和xml解析转换的px值一致。
 */
@Px
private fun Float.toRoundingPx(): Int {
    return (if (this >= 0) this + 0.5f else this - 0.5f).toInt()
}

@get:Px
val Int.dp: Int
    get() = toFloat().dp

@get:Px
val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toRoundingPx()

@Suppress("unused")
inline val View.matchParent: Int
    get() = ViewGroup.LayoutParams.MATCH_PARENT

@Suppress("unused")
inline val View.wrapContent: Int
    get() = ViewGroup.LayoutParams.WRAP_CONTENT

fun View.overScrollNever() {
    overScrollMode = View.OVER_SCROLL_NEVER
}

inline fun View.withLayoutParams(width: Int, height: Int, block: ViewGroup.MarginLayoutParams.() -> Unit = {}) {
    layoutParams = ViewGroup.MarginLayoutParams(width, height).apply(block)
}

inline fun View.onClick(crossinline block: () -> Unit) {
    setOnClickListener { block() }
}

inline fun ViewPager2.registerOnPageChangeCallback(
    crossinline onScrolled: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit = { _, _, _ -> },
    crossinline onSelected: (position: Int) -> Unit = {},
    crossinline onScrollStateChanged: (state: Int) -> Unit = {}
): ViewPager2.OnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) = onScrolled(position, positionOffset, positionOffsetPixels)

    override fun onPageSelected(position: Int) = onSelected(position)

    override fun onPageScrollStateChanged(state: Int) = onScrollStateChanged(state)
}.also(::registerOnPageChangeCallback)