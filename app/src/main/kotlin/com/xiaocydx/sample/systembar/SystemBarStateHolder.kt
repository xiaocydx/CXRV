package com.xiaocydx.sample.systembar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.systembar.SystemBar.Companion.InitialColor

/**
 * // TODO: 2023/12/22 验证重建表现
 *
 * @author xcc
 * @date 2023/12/21
 */
internal class SystemBarStateHolder(savaStateHandle: SavedStateHandle) : ViewModel() {
    private val stack: ArrayList<String>
    private val store = mutableMapOf<String, AppearanceLightState>()

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

    fun createState(who: String) = store.getOrPut(who) { AppearanceLightState() }

    fun resumeState(who: String): AppearanceLightState? {
        val index = stack.indexOf(who)
        if (index == -1) {
            stack.add(who)
            return store[who]
        }
        return if (isLastIndex(index)) store[who] else null
    }

    fun destroyState(who: String): AppearanceLightState? {
        var prevState: AppearanceLightState? = null
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

internal class AppearanceLightState(
    var navigationBarColor: Int = InitialColor,
    var appearanceLightStatusBar: Boolean = false,
    var appearanceLightNavigationBar: Boolean = false
)