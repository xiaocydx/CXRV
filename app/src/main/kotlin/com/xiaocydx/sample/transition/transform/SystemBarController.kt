/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PackageDirectoryMismatch")

package androidx.fragment.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xiaocydx.sample.R
import com.xiaocydx.sample.consume
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent

/**
 * 去除`window.decorView`实现的SystemBar间距和背景色，自行处理[WindowInsets]
 */
fun SystemBarController.Companion.init(window: Window) {
    // 设置decorFitsSystemWindows = false的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/kqx6tak191xz1zpv
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 设置softInputMode = SOFT_INPUT_ADJUST_RESIZE的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/ifiu0ptqnm080gzl
    @Suppress("DEPRECATION")
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    // Android 9.0以下decorView.onApplyWindowInsets()会返回新创建的WindowInsets，
    // 不会引用ViewRootImpl的成员属性mDispatchContentInsets和mDispatchStableInsets,
    // 若不返回decorView新创建的WindowInsets，则需要兼容WindowInsets可变引起的问题，
    // 详细解释：https://www.yuque.com/u12192380/khwdgb/yvtolsepi5kmz38i
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
        val decorInsets = insets.consume(statusBars() or navigationBars())
        ViewCompat.onApplyWindowInsets(window.decorView, decorInsets)
        insets
    }
}

/**
 * 简易的SystemBar控制方案
 *
 * 此类是单Activity处理[WindowInsets]的示例代码，实景场景需要更容易配置和管理的方案。
 */
class SystemBarController(private val fragment: Fragment) {
    private var pendingTypeMask = 0
    private var pendingStatusBarEdgeToEdge = false
    private var pendingGestureNavBarEdgeToEdge = false
    private var pendingStatusBarColor: Int? = null
    private var pendingNavigationBarColor: Int? = null
    private var pendingAppearanceLightStatusBar: Boolean? = null
    private var pendingAppearanceLightNavigationBar: Boolean? = null
    private var observer: AppearanceLightSystemsBarObserver? = null
    private var container: SystemBarContainer? = null
    private val window: Window?
        get() = fragment.activity?.window

    init {
        fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
            if (owner == null) {
                container = null
                observer?.remove()
                observer = null
                return@observeForever
            }
            if (container != null) return@observeForever
            // 执行顺序：
            // 1. fragment.mView = fragment.onCreateView()
            // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
            // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
            // 第3步的分发过程将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
            val window = requireNotNull(window) { "Fragment生命周期状态转换出现异常情况" }
            val view = requireNotNull(fragment.mView) { "Fragment生命周期状态转换出现异常情况" }
            require(view.parent == null) { "Fragment.view已有parent，不支持替换parent" }
            require(view !is SystemBarContainer) { "Fragment只能关联一个SystemBarController" }
            container = SystemBarContainer(fragment.requireContext())
            observer = AppearanceLightSystemsBarObserver(window, owner.lifecycle, container!!)
            fragment.mView = container!!.attach(view).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner as? ViewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(owner as? SavedStateRegistryOwner)
            }
            window.recordSystemBarColorIfNecessary()
            consumeTypeMask(pendingTypeMask)
            setStatusBarEdgeToEdge(pendingStatusBarEdgeToEdge)
            setGestureNavBarEdgeToEdge(pendingGestureNavBarEdgeToEdge)
            setStatusBarColorInternal(pendingStatusBarColor)
            setNavigationBarColorInternal(pendingNavigationBarColor)
            setAppearanceLightStatusBarInternal(pendingAppearanceLightStatusBar)
            setAppearanceLightNavigationBarInternal(pendingAppearanceLightNavigationBar)
        }
    }

    fun consumeStatusBar() = consumeTypeMask(statusBars())

    fun consumeNavigationBar() = consumeTypeMask(navigationBars())

    fun setStatusBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingStatusBarEdgeToEdge = edgeToEdge
        container?.statusBarEdgeToEdge = edgeToEdge
    }

    fun setGestureNavBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingGestureNavBarEdgeToEdge = edgeToEdge
        container?.gestureNavBarEdgeToEdge = edgeToEdge
    }

    fun setStatusBarColor(@ColorInt color: Int) = setStatusBarColorInternal(color)

    fun setNavigationBarColor(@ColorInt color: Int) = setNavigationBarColorInternal(color)

    fun setAppearanceLightStatusBar(isLight: Boolean) = setAppearanceLightStatusBarInternal(isLight)

    fun setAppearanceLightNavigationBar(isLight: Boolean) = setAppearanceLightNavigationBarInternal(isLight)

    private fun consumeTypeMask(typeMask: Int) = apply {
        pendingTypeMask = pendingTypeMask or typeMask
        container?.consumeTypeMask = pendingTypeMask
    }

    private fun setStatusBarColorInternal(color: Int?) = apply {
        pendingStatusBarColor = color
        val finalColor = color ?: window?.initialStatusBarColor
        if (finalColor != null) container?.statusBarColor = finalColor
    }

    private fun setNavigationBarColorInternal(color: Int?) = apply {
        pendingNavigationBarColor = color
        val finalColor = color ?: window?.initialNavigationBarColor
        if (finalColor != null) container?.navigationBarColor = finalColor
    }

    private fun setAppearanceLightStatusBarInternal(isLight: Boolean?) = apply {
        pendingAppearanceLightStatusBar = isLight
        if (isLight != null) observer?.isAppearanceLightStatusBar = isLight
    }

    private fun setAppearanceLightNavigationBarInternal(isLight: Boolean?) = apply {
        pendingAppearanceLightNavigationBar = isLight
        if (isLight != null) observer?.isAppearanceLightNavigationBar = isLight
    }

    companion object
}

private fun Window.recordSystemBarColorIfNecessary() {
    // 记录StatusBar和NavigationBar的初始背景色，
    // 执行完decorView创建流程，才能获取到背景色。
    initialStatusBarColor
    initialNavigationBarColor
}

private val Window.initialStatusBarColor: Int
    get() {
        val key = R.id.tag_window_initial_status_bar_color
        var color = decorView.getTag(key) as? Int
        if (color == null) {
            color = statusBarColor
            decorView.setTag(key, color)
        }
        return color
    }

private val Window.initialNavigationBarColor: Int
    get() {
        val key = R.id.tag_window_initial_navigation_bar_color
        var color = decorView.getTag(key) as? Int
        if (color == null) {
            color = navigationBarColor
            decorView.setTag(key, color)
        }
        return color
    }

private class SystemBarContainer(context: Context) : FrameLayout(context) {
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var contentView: View? = null

    var consumeTypeMask: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var statusBarEdgeToEdge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var gestureNavBarEdgeToEdge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var statusBarColor: Int
        get() = statusBarDrawable.color
        set(value) {
            if (statusBarDrawable.color == value) return
            statusBarDrawable.color = value
            invalidate()
        }

    var navigationBarColor: Int
        get() = navigationBarDrawable.color
        set(value) {
            if (navigationBarDrawable.color == value) return
            navigationBarDrawable.color = value
            invalidate()
        }

    fun attach(view: View) = apply {
        setWillNotDraw(false)
        removeAllViews()
        contentView = view
        addView(contentView, matchParent, matchParent)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val applyInsets = insets.consume(consumeTypeMask)
            val statusBars = applyInsets.getInsets(statusBars())
            val navigationBars = applyInsets.getInsets(navigationBars())
            updatePadding(
                top = if (statusBarEdgeToEdge) 0 else statusBars.top,
                bottom = when {
                    !gestureNavBarEdgeToEdge -> navigationBars.bottom
                    else -> if (applyInsets.isGestureNavigationBar(resources)) 0 else navigationBars.bottom
                }
            )
            applyInsets
        }
        doOnAttach(ViewCompat::requestApplyInsets)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        checkContentView()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        checkContentView()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        checkContentView()
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, paddingTop)
        navigationBarDrawable.setBounds(0, height - paddingBottom, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun checkContentView() {
        check(childCount <= 1)
        if (childCount == 1) check(getChildAt(0) === contentView)
    }
}

private class AppearanceLightSystemsBarObserver(
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val container: SystemBarContainer,
    private val state: Lifecycle.State = RESUMED
) : LifecycleEventObserver, View.OnAttachStateChangeListener {
    private var currentState = lifecycle.currentState
    private val controller = WindowInsetsControllerCompat(window, window.decorView)

    var isAppearanceLightStatusBar = controller.isAppearanceLightStatusBars
        set(value) {
            field = value
            updateAppearanceLightSystemsBar()
        }

    var isAppearanceLightNavigationBar = controller.isAppearanceLightNavigationBars
        set(value) {
            field = value
            updateAppearanceLightSystemsBar()
        }

    init {
        container.addOnAttachStateChangeListener(this)
        if (container.isAttachedToWindow) onViewAttachedToWindow(container)
    }

    fun remove() {
        container.removeOnAttachStateChangeListener(this)
        onViewDetachedFromWindow(container)
    }

    override fun onViewAttachedToWindow(v: View) {
        lifecycle.addObserver(this)
    }

    override fun onViewDetachedFromWindow(v: View) {
        lifecycle.removeObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        currentState = lifecycle.currentState
        if (currentState == DESTROYED) remove()
        updateAppearanceLightSystemsBar()
    }

    private fun updateAppearanceLightSystemsBar() {
        if (!container.isAttachedToWindow || !currentState.isAtLeast(state)) return
        if (controller.isAppearanceLightStatusBars != isAppearanceLightStatusBar) {
            controller.isAppearanceLightStatusBars = isAppearanceLightStatusBar
        }
        if (controller.isAppearanceLightNavigationBars != isAppearanceLightNavigationBar) {
            controller.isAppearanceLightNavigationBars = isAppearanceLightNavigationBar
        }
        if (window.navigationBarColor != container.navigationBarColor) {
            // 设置navigationBarColor，AppearanceLightNavigationBar才会自适应，
            // 同时也能支持Android 6.0及以上设置AppearanceLightNavigationBar。
            window.navigationBarColor = container.navigationBarColor
        }
    }
}