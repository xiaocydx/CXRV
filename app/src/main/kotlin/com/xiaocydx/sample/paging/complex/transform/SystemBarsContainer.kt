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

package com.xiaocydx.sample.paging.complex.transform

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.viewLifecycle

/**
 * 简易的SystemBars容器
 *
 * 此类只是单Activity处理[WindowInsets]的示例代码，
 * 在实景场景中，应当选择更容易配置和管理的处理方案。
 *
 * @author xcc
 * @date 2023/8/4
 */
class SystemBarsContainer(context: Context) : FrameLayout(context) {
    private var statusBarHeight = 0
    private var navigationBarHeight = 0
    private var statusBarEdgeToEdge = false
    private var gestureNavBarEdgeToEdge = false
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var observer: AppearanceLightStatusBarObserver? = null
    private var contentView: View? = null

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

    fun setLightStatusBarOnResume(window: Window, lifecycle: Lifecycle) = apply {
        AppearanceLightStatusBarObserver(lifecycle, isDarkStatusBar = false).attach(window)
    }

    fun setDarkStatusBarOnResume(window: Window, lifecycle: Lifecycle) = apply {
        AppearanceLightStatusBarObserver(lifecycle, isDarkStatusBar = true).attach(window)
    }

    fun attach(view: View) = apply {
        setWillNotDraw(false)
        removeAllViews()
        contentView = view
        addView(contentView, matchParent, matchParent)
        doOnApplyWindowInsets { _, insets, _ ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            updateSystemBars(
                statusBarHeight = if (statusBarEdgeToEdge) 0 else statusBars.top,
                navigationBarHeight = when {
                    !gestureNavBarEdgeToEdge -> navigationBars.bottom
                    else -> if (insets.isGestureNavigationBar(resources)) 0 else navigationBars.bottom
                }
            )
        }
    }

    private fun updateSystemBars(statusBarHeight: Int, navigationBarHeight: Int) {
        val statusBarChanged = this.statusBarHeight != statusBarHeight
        val navigationBarChanged = this.navigationBarHeight != navigationBarHeight
        if (statusBarChanged || navigationBarChanged) {
            contentView?.updateLayoutParams<LayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navigationBarHeight
            }
            this.statusBarHeight = statusBarHeight
            this.navigationBarHeight = navigationBarHeight
            invalidate()
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
        statusBarDrawable.setBounds(0, 0, width, statusBarHeight)
        navigationBarDrawable.setBounds(0, height - navigationBarHeight, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun checkOnlyContentView() {
        check(childCount <= 1)
        check(getChildAt(0) === contentView)
    }

    private inner class AppearanceLightStatusBarObserver(
        private val lifecycle: Lifecycle,
        private val isDarkStatusBar: Boolean
    ) : LifecycleEventObserver {
        private var controller: WindowInsetsControllerCompat? = null

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) detach()
            val controller = controller
            val currentState = source.lifecycle.currentState
            if (controller == null || currentState != Lifecycle.State.RESUMED) return
            if (controller.isAppearanceLightStatusBars != isDarkStatusBar) {
                controller.isAppearanceLightStatusBars = isDarkStatusBar
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

    companion object
}

@Suppress("DEPRECATION")
fun SystemBarsContainer.Companion.disableDecorFitsSystemWindows(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    // 拦截DecorView.onApplyWindowInsets()处理WindowInsets的逻辑，
    // 注意：以下拦截方式仅作为示例代码，Android 9.0以下WindowInsets是可变的，
    // Android 9.0以下DecorView.onApplyWindowInsets()返回新创建的WindowInsets，
    // 不会引用ViewRootImpl的成员属性mDispatchContentInsets和mDispatchStableInsets（变的不可变）,
    // 若不返回DecorView新创建的WindowInsets，则需要兼容WindowInsets可变引起的问题（确保不可变）。
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets -> insets }
}

fun SystemBarsContainer.setLightStatusBarOnResume(activity: ComponentActivity) =
        setLightStatusBarOnResume(activity.window, activity.lifecycle)

fun SystemBarsContainer.setLightStatusBarOnResume(fragment: Fragment) =
        setLightStatusBarOnResume(fragment.requireActivity().window, fragment.viewLifecycle)

fun SystemBarsContainer.setDarkStatusBarOnResume(activity: ComponentActivity) =
        setDarkStatusBarOnResume(activity.window, activity.lifecycle)

fun SystemBarsContainer.setDarkStatusBarOnResume(fragment: Fragment) =
        setDarkStatusBarOnResume(fragment.requireActivity().window, fragment.viewLifecycle)

fun SystemBarsContainer.setWindowSystemBarsColor(fragment: Fragment) =
        setWindowSystemBarsColor(fragment.requireActivity().window)

fun SystemBarsContainer.setWindowStatusBarColor(fragment: Fragment) =
        setWindowStatusBarColor(fragment.requireActivity().window)

fun SystemBarsContainer.setWindowNavigationBarColor(fragment: Fragment) =
        setWindowNavigationBarColor(fragment.requireActivity().window)

fun SystemBarsContainer.setWindowSystemBarsColor(window: Window) = apply {
    setStatusBarColor(window.statusBarColor)
    setNavigationBarColor(window.navigationBarColor)
}

fun SystemBarsContainer.setWindowStatusBarColor(window: Window) = apply {
    // 执行完decorView的创建流程，才能获取到颜色值
    window.decorView
    setStatusBarColor(window.statusBarColor)
}

fun SystemBarsContainer.setWindowNavigationBarColor(window: Window) = apply {
    // 执行完decorView的创建流程，才能获取到颜色值
    window.decorView
    setNavigationBarColor(window.navigationBarColor)
}