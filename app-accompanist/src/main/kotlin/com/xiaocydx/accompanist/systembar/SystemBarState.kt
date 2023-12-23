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

package com.xiaocydx.accompanist.systembar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.xiaocydx.accompanist.systembar.SystemBarContainer.Companion.InitialColor

/**
 * @author xcc
 * @date 2023/12/21
 */
internal class SystemBarState(
    var isAppearanceLightStatusBar: Boolean = false,
    var isAppearanceLightNavigationBar: Boolean = false,
    var navigationBarColor: Int = InitialColor
)

internal class SystemBarStateHolder(savaStateHandle: SavedStateHandle) : ViewModel() {
    private val stack: ArrayList<String>
    private val store = mutableMapOf<String, SystemBarState>()

    init {
        val value = savaStateHandle.get<ArrayList<String>>(STACK_KEY)
        if (value != null) {
            stack = value
        } else {
            stack = arrayListOf()
            savaStateHandle[STACK_KEY] = stack
        }
    }

    fun peekState(who: String) = store[who]

    fun createState(who: String) = store.getOrPut(who) { SystemBarState() }

    // TODO: 确保在activity.savedSave = true之前添加
    fun resumeState(who: String): SystemBarState? {
        val index = stack.indexOf(who)
        if (index == -1) {
            stack.add(who)
            return store[who]
        }
        return if (isLastIndex(index)) store[who] else null
    }

    // TODO: 确保在activity.savedSave = true之前移除
    fun destroyState(who: String): SystemBarState? {
        var prevState: SystemBarState? = null
        val index = stack.indexOf(who)
        if (isLastIndex(index)) {
            val prev = stack.getOrNull(index - 1)
            if (prev != null) prevState = store[prev]
        }
        if (index in stack.indices) stack.removeAt(index)
        return prevState
    }

    private fun isLastIndex(index: Int) = index >= 0 && index == stack.lastIndex

    private companion object {
        const val STACK_KEY = "com.xiaocydx.sample.systembar.STACK_KEY"
    }
}