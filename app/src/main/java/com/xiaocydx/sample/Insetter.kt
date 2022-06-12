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
fun Window.navigationEdgeToEdge(@ColorInt navigationBarColor: Int = 0x33000000) {
    this.navigationBarColor = navigationBarColor

    setDecorFitsSystemWindowsCompat(decorFitsSystemWindows = false)
    decorView.doOnApplyWindowInsetsCompat { view, insets, _ ->
        val isGestureNavigation = insets.isGestureNavigation(view.resources)
        view.onApplyWindowInsetsCompat(when {
            isGestureNavigation -> insets.modifyInsets(
                typeMask = WindowInsetsCompat.Type.navigationBars(),
                newInsets = { copy(bottom = 0) }
            )
            else -> insets
        })
    }

    // 初始化时，contentParent.marginTop为0，后续可能赋值为ActionBar的高度，
    // 因此通过设置contentParent.paddingTop，补充回statusBarInsets.top。
    val contentParent: View = findViewById(android.R.id.content)
    contentParent.doOnApplyWindowInsetsCompat apply@{ view, _, initialState ->
        // 1.ActionBarOverlayLayout.onApplyWindowInsets()会消费WindowInsets，
        // 2.ActionBarOverlayLayout.dispatchApplyWindowInsets()停止对子View分发WindowInsets，
        // 3.ActionBarOverlayLayout.onMeasure()仍会对子View分发WindowInsets，但不是原始值。
        // 因此需要获取分发到视图树的RootWindowInsets，才能得到statusBarInsets.top的原始值。
        val applyInsets = view.getRootWindowInsetsCompat() ?: return@apply
        view.updatePadding(top = applyInsets.getStatusBarHeight() + initialState.paddings.top)
    }
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

fun View.doOnApplyRootWindowInsetsCompat(
    block: (view: View, rootInsets: WindowInsetsCompat, initialState: ViewState) -> Unit
) {
    val initialState = ViewState(this)
    setOnApplyWindowInsetsListenerCompat { view, insets ->
        view.getRootWindowInsetsCompat()
            ?.let { block(view, it, initialState) }
        insets
    }
    doOnAttach { view ->
        view.getRootWindowInsetsCompat()
            ?.let { block(view, it, initialState) }
        view.requestApplyInsetsCompat()
    }
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

fun WindowInsetsCompat.isGestureNavigation(resources: Resources): Boolean {
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
