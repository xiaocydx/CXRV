@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.sample

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.CheckResult
import androidx.annotation.Px
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.enableBoundCheckCompat
import java.lang.ref.WeakReference

/**
 * 启用手势导航栏边到边的示例代码
 */
fun Window.enableGestureNavBarEdgeToEdge() {
    val contentRef = (decorView as ViewGroup).children
        .firstOrNull { it is ViewGroup }?.let(::WeakReference)
    WindowCompat.setDecorFitsSystemWindows(this, false)
    ViewCompat.setOnApplyWindowInsetsListener(decorView) { v, insets ->
        var decorInsets = insets
        var applyInsets = insets
        if (insets.isGestureNavigationBar(v.resources)) {
            decorInsets = insets.consume(navigationBars())
        } else {
            applyInsets = insets.consume(navigationBars())
        }
        ViewCompat.onApplyWindowInsets(v, decorInsets)
        contentRef?.get()?.updateMargins(
            top = decorInsets.getInsets(statusBars()).top,
            bottom = decorInsets.getInsets(navigationBars()).bottom
        )
        applyInsets
    }
    decorView.doOnAttach(ViewCompat::requestApplyInsets)
}

fun RecyclerView.enableGestureNavBarEdgeToEdge() {
    clipToPadding = false
    layoutManager?.enableBoundCheckCompat()
    doOnApplyWindowInsets { view, insets, initialState ->
        val navigationBars = insets.getInsets(navigationBars())
        view.updatePadding(bottom = navigationBars.bottom + initialState.paddings.bottom)
    }
}

fun View.doOnApplyWindowInsets(
    block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit
) {
    val initialState = ViewState(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets, initialState)
        insets
    }
    doOnAttach(ViewCompat::requestApplyInsets)
}

private fun View.updateMargins(
    left: Int = marginLeft,
    top: Int = marginTop,
    right: Int = marginRight,
    bottom: Int = marginBottom
) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val changed = left != marginLeft || top != marginTop
            || right != marginTop || bottom != marginBottom
    params.setMargins(left, top, right, bottom)
    if (changed) layoutParams = params
}

@CheckResult
private fun WindowInsetsCompat.consume(typeMask: Int): WindowInsetsCompat {
    return WindowInsetsCompat.Builder(this).setInsets(typeMask, Insets.NONE).build()
}

private fun WindowInsetsCompat.isGestureNavigationBar(resources: Resources): Boolean {
    val threshold = (24 * resources.displayMetrics.density).toInt()
    val stableHeight = getInsetsIgnoringVisibility(navigationBars()).bottom
    return stableHeight <= threshold.coerceAtLeast(66)
}

/**
 * 视图的初始状态
 */
data class ViewState internal constructor(
    /**
     * 视图的初始params
     */
    val params: ViewParams,

    /**
     * 视图的初始paddings
     */
    val paddings: ViewPaddings,
) {
    internal constructor(view: View) : this(ViewParams(view), ViewPaddings(view))
}

/**
 * 视图的初始params
 */
data class ViewParams internal constructor(
    @Px val width: Int,
    @Px val height: Int,
    @Px val marginLeft: Int,
    @Px val marginTop: Int,
    @Px val marginRight: Int,
    @Px val marginBottom: Int
) {
    internal constructor(view: View) : this(
        width = view.layoutParams?.width ?: 0,
        height = view.layoutParams?.height ?: 0,
        marginLeft = view.marginLeft,
        marginTop = view.marginTop,
        marginRight = view.marginRight,
        marginBottom = view.marginBottom
    )
}

/**
 * 视图的初始paddings
 */
data class ViewPaddings internal constructor(
    @Px val left: Int,
    @Px val top: Int,
    @Px val right: Int,
    @Px val bottom: Int
) {
    internal constructor(view: View) : this(
        left = view.paddingLeft,
        top = view.paddingTop,
        right = view.paddingRight,
        bottom = view.paddingBottom
    )
}