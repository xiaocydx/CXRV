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

import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xiaocydx.accompanist.systembar.SystemBarContainer
import com.xiaocydx.accompanist.systembar.SystemBarContainer.Companion.InitialColor

/**
 * 负责应用和恢复[SystemBarState]
 *
 * @author xcc
 * @date 2023/12/22
 */
internal class SystemBarStateObserver private constructor(
    private val who: String,
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val activeState: Lifecycle.State,
    private val container: SystemBarContainer,
    private val stateHolder: SystemBarStateHolder,
    private val isSaveStated: () -> Boolean,
    private val canRemoveState: () -> Boolean
) : LifecycleEventObserver {
    private val controller = WindowInsetsControllerCompat(window, window.decorView)

    init {
        if (lifecycle.currentState !== DESTROYED) {
            stateHolder.ensureState(who, isSaveStated())
            lifecycle.addObserver(this)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        val applyEvent = Lifecycle.Event.upTo(activeState)
        val removeEvent = Lifecycle.Event.ON_DESTROY
        when (event) {
            applyEvent -> applyCurrentState()
            removeEvent -> remove()
            else -> return
        }
    }

    fun remove() {
        lifecycle.removeObserver(this)
        if (canRemoveState()) {
            stateHolder.applyPrevState(who)?.let(::applyState)
            stateHolder.removeState(who, isSaveStated())
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
        if (lifecycle.currentState.isAtLeast(activeState)) {
            stateHolder.applyState(who)?.let(::applyState)
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
        private val FragmentActivity.isStateSaved: Boolean
            get() = supportFragmentManager.isStateSaved
        private val FragmentActivity.stateHolder: SystemBarStateHolder
            get() = ViewModelProvider(this)[SystemBarStateHolder::class.java]

        fun create(
            activity: FragmentActivity,
            container: SystemBarContainer
        ) = SystemBarStateObserver(
            who = "Activity",
            window = activity.window,
            lifecycle = activity.lifecycle,
            activeState = STARTED,
            container = container,
            stateHolder = activity.stateHolder,
            isSaveStated = { activity.isStateSaved },
            canRemoveState = { false }
        )

        fun create(
            fragment: Fragment,
            container: SystemBarContainer
        ) = SystemBarStateObserver(
            who = fragment.mWho,
            window = fragment.requireActivity().window,
            lifecycle = fragment.lifecycle,
            activeState = RESUMED,
            container = container,
            stateHolder = fragment.requireActivity().stateHolder,
            isSaveStated = { fragment.requireActivity().isStateSaved },
            canRemoveState = { !fragment.isAdded && !fragment.isInBackStack }
        )
    }
}

internal class SystemBarState(
    var isApplied: Boolean = false,
    var isAppearanceLightStatusBar: Boolean = false,
    var isAppearanceLightNavigationBar: Boolean = false,
    var navigationBarColor: Int = InitialColor
)

internal class SystemBarStateHolder(savaStateHandle: SavedStateHandle) : ViewModel() {
    /**
     * `savaStateHandle`仅保存[backStack]，不需要保存[stateStore]，
     * 当页面重建时，会再次调用[ensureState]构建[SystemBarState]，
     * 页面构造阶段声明的配置，会通过[peekState]进行赋值。
     */
    private val backStack: ArrayList<String>
    private val stateStore = mutableMapOf<String, SystemBarState>()

    init {
        val value = savaStateHandle.get<ArrayList<String>>(BACK_STACK_KEY)
        if (value != null) {
            backStack = value
        } else {
            backStack = arrayListOf()
            savaStateHandle[BACK_STACK_KEY] = backStack
        }
    }

    fun ensureState(who: String, isStateSaved: Boolean) {
        checkStateSaved(isStateSaved)
        if (!backStack.contains(who)) {
            backStack.add(who)
        }
        var state = stateStore[who]
        if (state == null) {
            state = SystemBarState()
            stateStore[who] = state
        }
        // isApplied用于避免先应用默认值，再应用当前值
        state.isApplied = false
    }

    fun peekState(who: String): SystemBarState? {
        return stateStore[who]
    }

    fun applyState(who: String): SystemBarState? {
        val index = backStack.indexOf(who)
        val state = stateStore[who]
        state?.isApplied = true
        return if (isLastApply(index)) state else null
    }

    fun applyPrevState(who: String): SystemBarState? {
        val index = backStack.indexOf(who)
        var prevState: SystemBarState? = null
        if (isLastApply(index)) {
            val prevWho = backStack.getOrNull(index - 1)
            if (prevWho != null) prevState = stateStore[prevWho]
        }
        return prevState?.takeIf { it.isApplied }
    }

    fun removeState(who: String, isStateSaved: Boolean): SystemBarState? {
        checkStateSaved(isStateSaved)
        backStack.remove(who)
        return stateStore.remove(who)
    }

    private fun checkStateSaved(isStateSaved: Boolean) {
        check(!isStateSaved) { "SavedStateHandle已保存，不允许再修改" }
    }

    private fun isLastApply(index: Int): Boolean {
        return index >= 0 && index == backStack.lastIndex
    }

    private companion object {
        const val BACK_STACK_KEY = "com.xiaocydx.accompanist.systembar.BACK_STACK_KEY"
    }
}