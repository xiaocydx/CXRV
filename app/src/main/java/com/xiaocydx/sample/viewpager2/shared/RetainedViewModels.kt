package com.xiaocydx.sample.viewpager2.shared

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.clear
import androidx.lifecycle.setTagIfAbsent
import java.io.Closeable

/**
 * 通过[key]获取[ViewModel]，若不存在，则调用[defaultValue]保留结果。
 */
@MainThread
inline fun <VM : ViewModel> RetainedViewModels<VM>.getOrPut(key: String, defaultValue: () -> VM): VM {
    return getOrNull(key) ?: defaultValue().also { put(key, it) }
}

/**
 * 遍历[ViewModel]
 */
@MainThread
inline fun <VM : ViewModel> RetainedViewModels<VM>.forEach(action: (VM) -> Unit) {
    keys().forEach { getOrNull(it)?.let(action) }
}

/**
 * 获取第一个满足[predicate]的[ViewModel]
 */
@MainThread
inline fun <VM : ViewModel> RetainedViewModels<VM>.firstOrNull(
    predicate: (VM) -> Boolean
): VM? {
    forEach { if (predicate(it)) return it }
    return null
}

/**
 * 保留[keys]对应的[ViewModel]，移除其余的[ViewModel]
 */
@MainThread
fun <VM : ViewModel> RetainedViewModels<VM>.retainForKeys(keys: Set<String>) {
    if (isEmpty()) return
    if (keys.isEmpty()) {
        clear()
        return
    }
    val existedKeys = keys().toTypedArray()
    existedKeys.forEach { existedKey ->
        if (!keys.contains(existedKey)) {
            remove(existedKey)
        }
    }
}

/**
 * 可用于分组场景，父级[ViewModel]保留子级[ViewModel]
 */
class RetainedViewModels<VM : ViewModel>(host: ViewModel) {
    private var hostCleared = false
    private val viewModels: MutableMap<String, VM> = mutableMapOf()

    init {
        assertMainThread()
        // 若已调用host.clear()，则直接执行Cleaner.close()
        host.setTagIfAbsent(hashCode().toString(), Cleaner())
    }

    @MainThread
    fun isEmpty(): Boolean {
        assertMainThread()
        return viewModels.isEmpty()
    }

    @MainThread
    fun keys(): Set<String> {
        assertMainThread()
        return viewModels.keys
    }

    @MainThread
    fun getOrNull(key: String): VM? {
        assertMainThread()
        return viewModels[key]
    }

    @MainThread
    fun put(key: String, viewModel: VM) {
        assertMainThread()
        if (hostCleared) {
            viewModel.clear()
            return
        }
        viewModels.put(key, viewModel)?.clear()
    }

    @MainThread
    fun remove(key: String) {
        assertMainThread()
        viewModels.remove(key)?.clear()
    }

    @MainThread
    fun clear() {
        assertMainThread()
        viewModels.values.forEach { it.clear() }
        viewModels.clear()
    }

    private fun assertMainThread() {
        val isMainThread = Thread.currentThread() === Looper.getMainLooper().thread
        assert(isMainThread) { "只能在主线程中调用当前函数" }
    }

    /**
     * `host.clear()`被调用时，同步调用[clear]
     */
    private inner class Cleaner : Closeable {
        @MainThread
        override fun close() {
            hostCleared = true
            clear()
        }
    }
}