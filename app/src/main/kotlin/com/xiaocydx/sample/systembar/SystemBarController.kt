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

import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsControllerCompat
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
import com.xiaocydx.sample.systembar.InitialColor
import com.xiaocydx.sample.systembar.SystemBarContainer

/**
 * 简易的SystemBar控制方案
 *
 * 此类是单Activity处理[WindowInsets]的示例代码，实景场景需要更容易配置和管理的方案。
 *
 * @author xcc
 * @date 2023/12/21
 */
class SystemBarController(private val fragment: Fragment) {
    private var pendingConsumeTypeMask = 0
    private var pendingStatusBarEdgeToEdge = false
    private var pendingGestureNavBarEdgeToEdge = false
    private var pendingStatusBarColor: Int = InitialColor
    private var pendingNavigationBarColor: Int = InitialColor
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
            setConsumeTypeMask(pendingConsumeTypeMask)
            setStatusBarEdgeToEdge(pendingStatusBarEdgeToEdge)
            setGestureNavBarEdgeToEdge(pendingGestureNavBarEdgeToEdge)
            setStatusBarColor(pendingStatusBarColor)
            setNavigationBarColor(pendingNavigationBarColor)
            setAppearanceLightStatusBarInternal(pendingAppearanceLightStatusBar)
            setAppearanceLightNavigationBarInternal(pendingAppearanceLightNavigationBar)
        }
    }

    fun setConsumeStatusBar(consume: Boolean) = setConsumeTypeMask(run {
        val typeMask = pendingConsumeTypeMask
        if (consume) typeMask or statusBars() else typeMask and statusBars().inv()
    })

    fun setConsumeNavigationBar(consume: Boolean) = setConsumeTypeMask(run {
        val typeMask = pendingConsumeTypeMask
        if (consume) typeMask or navigationBars() else typeMask and navigationBars().inv()
    })

    fun setStatusBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingStatusBarEdgeToEdge = edgeToEdge
        container?.statusBarEdgeToEdge = edgeToEdge
    }

    fun setGestureNavBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        pendingGestureNavBarEdgeToEdge = edgeToEdge
        container?.gestureNavBarEdgeToEdge = edgeToEdge
    }

    fun setStatusBarColor(@ColorInt color: Int) = apply {
        pendingStatusBarColor = color
        if (color != InitialColor) {
            container?.statusBarColor = color
        } else if (window != null) {
            container?.statusBarColor = window!!.initialStatusBarColor
        }
    }

    fun setNavigationBarColor(@ColorInt color: Int) = apply {
        pendingNavigationBarColor = color
        if (color != InitialColor) {
            container?.navigationBarColor = color
        } else if (window != null) {
            container?.navigationBarColor = window!!.initialNavigationBarColor
        }
    }

    fun setAppearanceLightStatusBar(isLight: Boolean) = setAppearanceLightStatusBarInternal(isLight)

    fun setAppearanceLightNavigationBar(isLight: Boolean) = setAppearanceLightNavigationBarInternal(isLight)

    private fun setConsumeTypeMask(typeMask: Int) = apply {
        pendingConsumeTypeMask = typeMask
        container?.consumeTypeMask = pendingConsumeTypeMask
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