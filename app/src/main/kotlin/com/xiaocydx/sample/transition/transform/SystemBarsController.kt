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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent

/**
 * 简易的SystemBars控制方案
 *
 * 此类只是单Activity处理[WindowInsets]的示例代码，实景场景应当选择更容易配置和管理的方案。
 *
 * @author xcc
 * @date 2023/8/4
 */
class SystemBarsController(private val fragment: Fragment) {
    private var pendingAppearanceLightStatusBar = false
    private var pendingStatusBarEdgeToEdge = false
    private var pendingGestureNavBarEdgeToEdge = false
    private var pendingStatusBarColor: Int? = null
    private var pendingNavigationBarColor: Int? = null
    private var container: SystemBarsContainer? = null
    private val window: Window?
        get() = fragment.activity?.window

    init {
        fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
            if (owner == null) {
                container = null
                return@observeForever
            }
            // 执行顺序：
            // 1. fragment.mView = fragment.onCreateView()
            // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
            // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
            // 第3步的分发过程将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
            val view = requireNotNull(fragment.mView)
            assert(view.parent == null)
            container = SystemBarsContainer(fragment.requireContext())
            fragment.mView = container!!.attach(view).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner as? ViewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(owner as? SavedStateRegistryOwner)
            }
            setAppearanceLightStatusBar(pendingAppearanceLightStatusBar)
            setStatusBarEdgeToEdge(pendingStatusBarEdgeToEdge)
            setGestureNavBarEdgeToEdge(pendingGestureNavBarEdgeToEdge)
            setStatusBarColorInternal(pendingStatusBarColor)
            setNavigationBarColorInternal(pendingNavigationBarColor)
        }
    }

    fun setAppearanceLightStatusBar(isLight: Boolean) = apply {
        pendingAppearanceLightStatusBar = isLight
        val window = window ?: return@apply
        val owner = container?.findViewTreeLifecycleOwner() ?: return@apply
        container?.setAppearanceLightStatusBar(window, owner.lifecycle, isLight)
    }

    fun setStatusBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingStatusBarEdgeToEdge = edgeToEdge
        container?.setStatusBarEdgeToEdge(edgeToEdge)
    }

    fun setGestureNavBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingGestureNavBarEdgeToEdge = edgeToEdge
        container?.setGestureNavBarEdgeToEdge(edgeToEdge)
    }

    fun setStatusBarColor(@ColorInt color: Int) = setStatusBarColorInternal(color)

    fun setNavigationBarColor(@ColorInt color: Int) = setNavigationBarColorInternal(color)

    fun setWindowStatusBarColor() = setStatusBarColorInternal(null)

    fun setWindowNavigationBarColor() = setNavigationBarColorInternal(null)

    private fun setStatusBarColorInternal(color: Int?) = apply {
        pendingStatusBarColor = color
        // 执行完decorView的创建流程，才能获取到颜色值
        window?.decorView
        val finalColor = color ?: window?.statusBarColor
        if (finalColor != null) container?.setStatusBarColor(finalColor)
    }

    private fun setNavigationBarColorInternal(color: Int?) = apply {
        pendingNavigationBarColor = color
        // 执行完decorView的创建流程，才能获取到颜色值
        window?.decorView
        val finalColor = color ?: window?.navigationBarColor
        if (finalColor != null) container?.setNavigationBarColor(finalColor)
    }

    companion object
}

/**
 * 禁用`Window.decorView`对[WindowInsets]的处理，去除间距实现和系统栏背景色
 */
@Suppress("DEPRECATION")
fun SystemBarsController.Companion.disableDecorFitsSystemWindows(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    // 拦截DecorView.onApplyWindowInsets()处理WindowInsets的逻辑，
    // 注意：以下拦截方式仅作为示例代码，Android 9.0以下WindowInsets是可变的，
    // Android 9.0以下DecorView.onApplyWindowInsets()返回新创建的WindowInsets，
    // 不会引用ViewRootImpl的成员属性mDispatchContentInsets和mDispatchStableInsets（变的不可变）,
    // 若不返回DecorView新创建的WindowInsets，则需要兼容WindowInsets可变引起的问题（确保不可变）。
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets -> insets }
}

class SystemBarsContainer(context: Context) : FrameLayout(context) {
    private var statusBarEdgeToEdge = false
    private var gestureNavBarEdgeToEdge = false
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var observer: AppearanceLightStatusBarObserver? = null
    private var contentView: View? = null

    fun setAppearanceLightStatusBar(window: Window, lifecycle: Lifecycle, isLight: Boolean) {
        AppearanceLightStatusBarObserver(lifecycle, isLight).attach(window)
    }

    fun setStatusBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        if (statusBarEdgeToEdge != edgeToEdge) {
            statusBarEdgeToEdge = edgeToEdge
            ViewCompat.requestApplyInsets(this)
        }
    }

    fun setGestureNavBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        if (gestureNavBarEdgeToEdge != edgeToEdge) {
            gestureNavBarEdgeToEdge = edgeToEdge
            ViewCompat.requestApplyInsets(this)
        }
    }

    fun setStatusBarColor(@ColorInt color: Int) = apply {
        if (statusBarDrawable.color != color) {
            statusBarDrawable.color = color
            invalidate()
        }
    }

    fun setNavigationBarColor(@ColorInt color: Int) = apply {
        if (navigationBarDrawable.color != color) {
            navigationBarDrawable.color = color
            invalidate()
        }
    }

    fun attach(view: View) = apply {
        setWillNotDraw(false)
        removeAllViews()
        contentView = view
        addView(contentView, matchParent, matchParent)
        doOnApplyWindowInsets { _, insets, _ ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            updatePadding(
                top = if (statusBarEdgeToEdge) 0 else statusBars.top,
                bottom = when {
                    !gestureNavBarEdgeToEdge -> navigationBars.bottom
                    else -> if (insets.isGestureNavigationBar(resources)) 0 else navigationBars.bottom
                }
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        checkOnlyContentView()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        checkOnlyContentView()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        checkOnlyContentView()
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, paddingTop)
        navigationBarDrawable.setBounds(0, height - paddingBottom, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun checkOnlyContentView() {
        check(childCount <= 1)
        check(getChildAt(0) === contentView)
    }

    private inner class AppearanceLightStatusBarObserver(
        private val lifecycle: Lifecycle,
        private val isLight: Boolean
    ) : LifecycleEventObserver {
        private var controller: WindowInsetsControllerCompat? = null

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) detach()
            val controller = controller
            val currentState = source.lifecycle.currentState
            if (controller == null || currentState != Lifecycle.State.RESUMED) return
            if (controller.isAppearanceLightStatusBars != isLight) {
                controller.isAppearanceLightStatusBars = isLight
            }
        }

        fun attach(window: Window) {
            observer?.detach()
            controller = WindowInsetsControllerCompat(window, window.decorView)
            lifecycle.addObserver(this)
            observer = this
        }

        fun detach() {
            if (this === observer) observer = null
            lifecycle.removeObserver(this)
        }
    }
}