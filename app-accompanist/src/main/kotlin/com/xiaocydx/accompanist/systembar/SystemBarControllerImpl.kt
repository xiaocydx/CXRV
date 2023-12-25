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

@file:JvmName("SystemBarControllerImplInternalKt")
@file:Suppress("PackageDirectoryMismatch", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package androidx.fragment.app

import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleEventObserver
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
import com.xiaocydx.accompanist.systembar.hostName
import com.xiaocydx.accompanist.systembar.initialNavigationBarColor
import com.xiaocydx.accompanist.systembar.initialStatusBarColor
import com.xiaocydx.accompanist.systembar.name

/**
 * @author xcc
 * @date 2023/12/24
 */
internal sealed class SystemBarControllerImpl : SystemBarController {
    protected var container: SystemBarContainer? = null
    protected var observer: SystemBarStateObserver? = null
    protected abstract val window: Window?

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

    protected fun applyPendingSystemBarConfig() {
        statusBarColor = statusBarColor
        navigationBarColor = navigationBarColor
        statusBarEdgeToEdge = statusBarEdgeToEdge
        navigationBarEdgeToEdge = navigationBarEdgeToEdge
        isAppearanceLightStatusBar = isAppearanceLightStatusBar
        isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
    }

    fun attach(initializer: (SystemBarController.() -> Unit)? = null) =
            apply { initializer?.invoke(this) }.apply { onAttach() }

    protected abstract fun onAttach()
}

internal class ActivitySystemBarController(
    private val activity: FragmentActivity,
    private val repeatThrow: Boolean
) : SystemBarControllerImpl() {
    override val window: Window?
        get() = activity.window
    private val activityName: String
        get() = activity.javaClass.canonicalName ?: ""

    override fun onAttach() {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (!activity.lifecycle.currentState.isAtLeast(CREATED)) return
                activity.lifecycle.removeObserver(this)
                // activity.performCreate()的执行顺序：
                // 1. activity.onCreate() -> activity.setContentView()
                // 2. activity.dispatchActivityPostCreated()
                // 第2步执行到此处，将contentView的contentParent替换为container，
                // 此时containerId为android.R.id.content的Fragment还未创建view。
                // 第2步之后，不需要支持能再次调用activity.setContentView()，
                // 因为这是错误的调用情况，会导致已添加的Fragment.view被移除。
                container = createContainerThrowOrNull()
                if (container == null) {
                    // Activity构造阶段已创建SystemBarController，
                    // 后注入的SystemBarController不做任何处理。
                    return
                }
                observer = SystemBarStateObserver.create(activity, container!!)
                applyPendingSystemBarConfig()
            }
        })
    }

    private fun createContainerThrowOrNull(): SystemBarContainer? {
        val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
        for (index in 0 until contentParent.childCount) {
            if (contentParent.getChildAt(index) !is SystemBarContainer) continue
            check(!repeatThrow) { "${activityName}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(contentParent.context)
        while (contentParent.childCount > 0) {
            val child = contentParent.getChildAt(0)
            contentParent.removeViewAt(0)
            container.addView(child)
        }
        contentParent.addView(container)
        container.doOnAttach(ViewCompat::requestApplyInsets)
        return container
    }
}

internal class FragmentSystemBarController(
    private val fragment: Fragment,
    private val repeatThrow: Boolean
) : SystemBarControllerImpl() {
    override val window: Window?
        get() = fragment.activity?.window
    private val fragmentName: String
        get() = fragment.javaClass.canonicalName ?: ""

    override fun onAttach() {
        fragment.mViewLifecycleOwnerLiveData.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(owner: LifecycleOwner?) {
                if (owner == null) {
                    container = null
                    observer?.remove()
                    observer = null
                    return
                }
                if (container == null) {
                    // fragment.performCreateView()的执行顺序：
                    // 1. fragment.mView = fragment.onCreateView()
                    // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
                    // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
                    // 第3步执行到此处，将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
                    container = createContainerThrowOrNull(owner)
                    if (container == null) {
                        // Fragment构造阶段已创建SystemBarController，
                        // 后注入的SystemBarController不做任何处理。
                        fragment.viewLifecycleOwnerLiveData.removeObserver(this)
                        return
                    }
                    observer = SystemBarStateObserver.create(fragment, container!!)
                    applyPendingSystemBarConfig()
                }
            }
        })
    }

    private fun createContainerThrowOrNull(owner: LifecycleOwner): SystemBarContainer? {
        val activity = fragment.requireActivity()
        check(activity is SystemBar.Host) {
            "${activity.javaClass.canonicalName}需要实现${SystemBar.hostName}"
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

        val view = fragment.mView
        if (view is SystemBarContainer) {
            check(!repeatThrow) { "${fragmentName}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(view.context)
        fragment.mView = container.apply {
            addView(view)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner as? ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(owner as? SavedStateRegistryOwner)
            doOnAttach(ViewCompat::requestApplyInsets)
        }
        return container
    }
}