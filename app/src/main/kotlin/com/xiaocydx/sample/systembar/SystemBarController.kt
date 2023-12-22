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

@file:JvmName("SystemBarControllerInternalKt")
@file:Suppress("PackageDirectoryMismatch")
@file:SuppressLint("SupportAnnotationUsage")

package androidx.fragment.app

import android.annotation.SuppressLint
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xiaocydx.sample.systembar.SystemBarContainer
import com.xiaocydx.sample.systembar.SystemBarContainer.Companion.InitialColor
import com.xiaocydx.sample.systembar.initialNavigationBarColor
import com.xiaocydx.sample.systembar.initialStatusBarColor

/**
 * SystemBar控制器
 *
 * @author xcc
 * @date 2023/12/21
 */
class SystemBarController internal constructor(
    private val fragment: Fragment,
    private val repeatThrow: Boolean
) {
    private var container: SystemBarContainer? = null
    private var observer: SystemBarObserver? = null
    private val window: Window?
        get() = fragment.activity?.window

    init {
        val viewLifecycleOwnerLiveData = fragment.viewLifecycleOwnerLiveData
        viewLifecycleOwnerLiveData.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(owner: LifecycleOwner?) {
                if (owner == null) {
                    container = null
                    observer?.remove()
                    observer = null
                    return
                }
                if (container == null) {
                    // 执行顺序：
                    // 1. fragment.mView = fragment.onCreateView()
                    // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
                    // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
                    // 第3步的分发过程将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
                    require(owner.lifecycle.currentState === INITIALIZED)
                    val view = requireNotNull(fragment.mView) { "Fragment生命周期状态转换出现异常情况" }
                    require(view.parent == null) { "Fragment.view已有parent，不支持替换parent" }
                    container = createContainerThrowOrNull(view)
                    if (container == null) {
                        // Fragment构造阶段已创建SystemBarController，
                        // 后创建的SystemBarController不做任何处理。
                        viewLifecycleOwnerLiveData.removeObserver(this)
                        return
                    }

                    observer = SystemBarObserver.create(fragment, container!!)
                    fragment.mView = container!!.attach(view).apply {
                        setViewTreeLifecycleOwner(owner)
                        setViewTreeViewModelStoreOwner(owner as? ViewModelStoreOwner)
                        setViewTreeSavedStateRegistryOwner(owner as? SavedStateRegistryOwner)
                    }
                    applyPendingSystemBarConfig()
                }
            }
        })
    }

    var statusBarEdgeToEdge = false
        set(value) {
            field = value
            container?.statusBarEdgeToEdge = value
        }

    var gestureNavBarEdgeToEdge = false
        set(value) {
            field = value
            container?.gestureNavBarEdgeToEdge = value
        }

    @get:ColorInt
    @set:ColorInt
    var statusBarColor = InitialColor
        set(value) {
            field = value
            if (value != InitialColor) {
                container?.statusBarColor = value
            } else if (window != null) {
                container?.statusBarColor = window!!.initialStatusBarColor
            }
        }

    @get:ColorInt
    @set:ColorInt
    var navigationBarColor = InitialColor
        set(value) {
            field = value
            if (value != InitialColor) {
                observer?.setNavigationBarColor(value)
            } else if (window != null) {
                observer?.setNavigationBarColor(window!!.initialNavigationBarColor)
            }
        }

    var isAppearanceLightStatusBar = false
        set(value) {
            field = value
            observer?.setAppearanceLightStatusBar(value)
        }

    var isAppearanceLightNavigationBar = false
        set(value) {
            field = value
            observer?.setAppearanceLightNavigationBar(value)
        }

    private fun createContainerThrowOrNull(view: View): SystemBarContainer? {
        if (view !is SystemBarContainer) return SystemBarContainer(view.context)
        if (repeatThrow) throw IllegalArgumentException("Fragment只能关联一个SystemBarController")
        return null
    }

    private fun applyPendingSystemBarConfig() {
        statusBarEdgeToEdge = statusBarEdgeToEdge
        gestureNavBarEdgeToEdge = gestureNavBarEdgeToEdge
        statusBarColor = statusBarColor
        navigationBarColor = navigationBarColor
        isAppearanceLightStatusBar = isAppearanceLightStatusBar
        isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
    }
}