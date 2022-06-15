package com.xiaocydx.sample

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

@RequiresApi(21)
fun Window.navigationBarEdgeToEdge(
    isSupportButtonNavigationBar: Boolean = true,
    @ColorInt buttonNavigationBarColor: Int = 0x33000000
) {
    setDecorFitsSystemWindowsCompat(decorFitsSystemWindows = false)
    decorView.setOnApplyWindowInsetsListenerCompat apply@{ view, insets ->
        view.onApplyWindowInsetsCompat(when {
            !insets.isGestureNavigationBar(view.resources) -> insets
            else -> insets.consumeNavigationBarHeight()
        })
        insets
    }
    decorView.doOnAttach(View::requestApplyInsetsCompat)

    if (isSupportButtonNavigationBar) {
        navigationBarColor = buttonNavigationBarColor
    }
    val contentParent: View = findViewById(android.R.id.content)
    val initialState = ViewState(contentParent)
    contentParent.setOnApplyWindowInsetsListenerCompat { view, insets ->
        var applyInsets = insets
        var navigationBarHeight = 0
        if (!insets.isGestureNavigationBar(view.resources) && !isSupportButtonNavigationBar) {
            applyInsets = insets.consumeNavigationBarHeight()
            navigationBarHeight = insets.getNavigationBarHeight()
        }
        view.updatePadding(
            top = insets.getStatusBarHeight() + initialState.paddings.top,
            bottom = navigationBarHeight + initialState.paddings.bottom
        )
        applyInsets
    }
}

private fun WindowInsetsCompat.consumeNavigationBarHeight(): WindowInsetsCompat {
    return modifyInsets(WindowInsetsCompat.Type.navigationBars()) { copy(bottom = 0) }
}

fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

fun View.doOnApplyWindowInsetsCompat(
    block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit
) {
    val initialState = ViewState(this)
    setOnApplyWindowInsetsListenerCompat { view, insets ->
        block(view, insets, initialState)
        insets
    }
    doOnAttach(View::requestApplyInsetsCompat)
}

fun View.setOnApplyWindowInsetsListenerCompat(listener: OnApplyWindowInsetsListener?) {
    ViewCompat.setOnApplyWindowInsetsListener(this, listener)
}

fun View.onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
    ViewCompat.onApplyWindowInsets(this, insets)
}

fun View.getRootWindowInsetsCompat(): WindowInsetsCompat? {
    return ViewCompat.getRootWindowInsets(this)
}

fun View.requestApplyInsetsCompat() {
    ViewCompat.requestApplyInsets(this)
}

@CheckResult
fun Insets.copy(
    left: Int = this.left,
    top: Int = this.top,
    right: Int = this.right,
    bottom: Int = this.bottom
): Insets = Insets.of(left, top, right, bottom)

@CheckResult
inline fun WindowInsetsCompat.modifyInsets(
    @InsetsType typeMask: Int,
    newInsets: Insets.() -> Insets
): WindowInsetsCompat {
    val insets = newInsets(getInsets(typeMask))
    return newBuilder().setInsets(typeMask, insets).build()
}

@CheckResult
fun WindowInsetsCompat.newBuilder(): WindowInsetsCompat.Builder {
    return WindowInsetsCompat.Builder(this)
}

fun WindowInsetsCompat.getStatusBarInsets(): Insets {
    return getInsets(WindowInsetsCompat.Type.statusBars())
}

fun WindowInsetsCompat.getNavigationBarInsets(): Insets {
    return getInsets(WindowInsetsCompat.Type.navigationBars())
}

fun WindowInsetsCompat.getSystemsBarInsets(): Insets {
    return getInsets(WindowInsetsCompat.Type.systemBars())
}

fun WindowInsetsCompat.getStatusBarHeight(): Int {
    return getStatusBarInsets().top
}

fun WindowInsetsCompat.getNavigationBarHeight(): Int {
    return getNavigationBarInsets().bottom
}

fun WindowInsetsCompat.isGestureNavigationBar(resources: Resources): Boolean {
    val threshold = (24 * resources.displayMetrics.density).toInt()
    return getNavigationBarHeight() <= threshold.coerceAtLeast(66)
}

private val View.paddingDimensions: ViewDimensions
    get() = ViewDimensions(paddingLeft, paddingTop, paddingRight, paddingBottom)

private val View.marginDimensions: ViewDimensions
    get() = when (val lp: ViewGroup.LayoutParams? = layoutParams) {
        is ViewGroup.MarginLayoutParams -> {
            ViewDimensions(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
        }
        else -> ViewDimensions.EMPTY
    }

data class ViewState(
    val paddings: ViewDimensions = ViewDimensions.EMPTY,
    val margins: ViewDimensions = ViewDimensions.EMPTY
) {
    constructor(view: View) : this(
        paddings = view.paddingDimensions,
        margins = view.marginDimensions
    )
}

data class ViewDimensions(
    @Px val left: Int,
    @Px val top: Int,
    @Px val right: Int,
    @Px val bottom: Int
) {
    companion object {
        val EMPTY = ViewDimensions(0, 0, 0, 0)
    }
}
