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

@file:JvmName("SystemBarObserverInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.fragment.app

import android.view.View
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.xiaocydx.sample.systembar.SystemBarContainer
import com.xiaocydx.sample.systembar.SystemBarState
import com.xiaocydx.sample.systembar.SystemBarStateHolder
import com.xiaocydx.sample.viewLifecycle

/**
 * @author xcc
 * @date 2023/12/22
 */
internal class SystemBarObserver private constructor(
    private val who: String,
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val container: SystemBarContainer,
    private val stateHolder: SystemBarStateHolder,
    private val canDestroyState: () -> Boolean
) : LifecycleEventObserver, View.OnAttachStateChangeListener {
    private val controller = WindowInsetsControllerCompat(window, window.decorView)

    init {
        if (lifecycle.currentState !== DESTROYED) {
            stateHolder.createState(who)
            container.addOnAttachStateChangeListener(this)
            if (container.isAttachedToWindow) onViewAttachedToWindow(container)
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        lifecycle.addObserver(this)
    }

    override fun onViewDetachedFromWindow(v: View) {
        lifecycle.removeObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_RESUME -> applyCurrentState()
            ON_DESTROY -> remove()
            else -> return
        }
    }

    fun remove() {
        container.removeOnAttachStateChangeListener(this)
        onViewDetachedFromWindow(container)
        if (lifecycle.currentState === DESTROYED && canDestroyState()) {
            stateHolder.destroyState(who)?.let(::applyState)
        }
    }

    fun setAppearanceLightStatusBar(light: Boolean) {
        stateHolder.peekState(who)?.isAppearanceLightStatusBar = light
        applyCurrentState()
    }

    fun setAppearanceLightNavigationBar(light: Boolean) {
        stateHolder.peekState(who)?.isAppearanceLightNavigationBar = light
        applyCurrentState()
    }

    fun setNavigationBarColor(color: Int) {
        container.navigationBarColor = color
        stateHolder.peekState(who)?.navigationBarColor = color
        applyCurrentState()
    }

    private fun applyCurrentState() {
        if (container.isAttachedToWindow && lifecycle.currentState.isAtLeast(RESUMED)) {
            stateHolder.resumeState(who)?.let(::applyState)
        }
    }

    private fun applyState(state: SystemBarState) = with(state) {
        if (controller.isAppearanceLightStatusBars != isAppearanceLightStatusBar) {
            controller.isAppearanceLightStatusBars = isAppearanceLightStatusBar
        }
        if (controller.isAppearanceLightNavigationBars != isAppearanceLightNavigationBar) {
            controller.isAppearanceLightNavigationBars = isAppearanceLightNavigationBar
        }
        if (!isAppearanceLightNavigationBar && window.navigationBarColor != navigationBarColor) {
            // 部分机型设置navigationBarColor，isAppearanceLightNavigationBar = false才会生效，
            // 当state.navigationBarColor是InitialColor时，navigationBarColor可能会被特殊处理。
            window.navigationBarColor = navigationBarColor
        }
    }

    companion object {
        private val FragmentActivity.stateHolder: SystemBarStateHolder
            get() = ViewModelProvider(this)[SystemBarStateHolder::class.java]

        fun create(
            fragment: Fragment,
            container: SystemBarContainer
        ) = SystemBarObserver(
            who = fragment.mWho,
            window = fragment.requireActivity().window,
            lifecycle = fragment.viewLifecycle,
            container = container,
            stateHolder = fragment.requireActivity().stateHolder,
            canDestroyState = { !fragment.isAdded }
        )
    }
}