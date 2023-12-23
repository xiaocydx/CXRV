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

@file:JvmName("FragmentSystemBarControllerInternalKt")
@file:Suppress("PackageDirectoryMismatch", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package androidx.fragment.app

import android.view.View
import android.view.Window
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xiaocydx.accompanist.systembar.EdgeToEdge
import com.xiaocydx.accompanist.systembar.SystemBar
import com.xiaocydx.accompanist.systembar.SystemBarContainer
import com.xiaocydx.accompanist.systembar.SystemBarContainer.Companion.InitialColor
import com.xiaocydx.accompanist.systembar.SystemBarController
import com.xiaocydx.accompanist.systembar.applyPendingSystemBarConfig
import com.xiaocydx.accompanist.systembar.initialNavigationBarColor
import com.xiaocydx.accompanist.systembar.initialStatusBarColor
import com.xiaocydx.accompanist.systembar.name

/**
 * @author xcc
 * @date 2023/12/23
 */
internal class FragmentSystemBarController(
    private val fragment: Fragment,
    private val repeatThrow: Boolean
) : SystemBarController {
    private var container: SystemBarContainer? = null
    private var observer: SystemBarObserver? = null
    private val window: Window?
        get() = fragment.activity?.window
    private val fragmentName: String
        get() = fragment.javaClass.canonicalName ?: ""

    override var statusBarColor = InitialColor
        set(value) {
            field = value
            if (value != InitialColor) {
                container?.statusBarColor = value
            } else if (window != null) {
                container?.statusBarColor = window!!.initialStatusBarColor
            }
        }

    override var navigationBarColor = InitialColor
        set(value) {
            field = value
            if (value != InitialColor) {
                observer?.setNavigationBarColor(value)
            } else if (window != null) {
                observer?.setNavigationBarColor(window!!.initialNavigationBarColor)
            }
        }

    override var statusBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            field = value
            container?.statusBarEdgeToEdge = value
        }

    override var navigationBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            field = value
            container?.navigationBarEdgeToEdge = value
        }

    override var isAppearanceLightStatusBar = false
        set(value) {
            field = value
            observer?.setAppearanceLightStatusBar(value)
        }

    override var isAppearanceLightNavigationBar = false
        set(value) {
            field = value
            observer?.setAppearanceLightNavigationBar(value)
        }

    init {
        fragment.mViewLifecycleOwnerLiveData.observeForever(object : Observer<LifecycleOwner?> {
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
                    checkStateBeforeCreate(owner)
                    val view = fragment.mView!!
                    container = createContainerThrowOrNull(view)
                    if (container == null) {
                        // Fragment构造阶段已创建SystemBarController，
                        // 后创建的SystemBarController不做任何处理。
                        fragment.viewLifecycleOwnerLiveData.removeObserver(this)
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

    private fun checkStateBeforeCreate(owner: LifecycleOwner) {
        val activity = fragment.requireActivity()
        check(activity is SystemBar) {
            "${activity.javaClass.canonicalName}需要实现${SystemBar.name}"
        }
        check(owner.lifecycle.currentState === INITIALIZED) {
            "只能在${fragmentName}的构造阶段获取${SystemBarController.name}"
        }
        check(fragment.mView != null) {
            "${fragmentName}的生命周期状态转换出现异常情况"
        }
        check(fragment.mView.parent == null) {
            "${fragmentName}的view已有parent，不支持替换parent"
        }
    }

    private fun createContainerThrowOrNull(view: View): SystemBarContainer? {
        if (view !is SystemBarContainer) return SystemBarContainer(view.context)
        check(!repeatThrow) { "${fragmentName}只能关联一个${SystemBarController.name}" }
        return null
    }
}