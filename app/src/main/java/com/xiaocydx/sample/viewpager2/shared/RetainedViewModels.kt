package com.xiaocydx.sample.viewpager2.shared

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.clear
import androidx.lifecycle.setTagIfAbsent
import java.io.Closeable

/**
 * 保留[ViewModel]，可用于分类场景
 */
@Suppress("FunctionName")
@JvmName("RetainedViewModelsTyped")
fun RetainedViewModels(host: ViewModel): RetainedViewModels<ViewModel> {
    return RetainedViewModels<ViewModel>(host)
}

/**
 * 保留[ViewModel]，可用于分类场景
 */
@Suppress("FunctionName")
fun <VM : ViewModel> RetainedViewModels(host: ViewModel): RetainedViewModels<VM> {
    return RetainedViewModels<VM>().apply { host.setTagIfAbsent(hashCode().toString(), this) }
}

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
    val existedKeys = keys()
    existedKeys.forEach { existedKey ->
        if (!keys.contains(existedKey)) {
            remove(existedKey)
        }
    }
}

/**
 * 可用于分类场景，父级[ViewModel]保留子级[ViewModel]
 */
class RetainedViewModels<VM : ViewModel>
@PublishedApi internal constructor() : Closeable {
    private val viewModels: MutableMap<String, VM> = mutableMapOf()

    @MainThread
    fun isEmpty(): Boolean {
        return viewModels.isEmpty()
    }

    @MainThread
    fun keys(): Set<String> {
        return viewModels.keys
    }

    @MainThread
    fun getOrNull(key: String): VM? = viewModels[key]

    @MainThread
    fun put(key: String, viewModel: VM) {
        viewModels.put(key, viewModel)?.clear()
    }

    @MainThread
    fun remove(key: String) {
        viewModels.remove(key)?.clear()
    }

    @MainThread
    fun clear() {
        viewModels.values.forEach { it.clear() }
        viewModels.clear()
    }

    /**
     * [ViewModel.clear]被调用时，会同步调用[close]
     */
    @MainThread
    @SinceKotlin("999.9")
    @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
    override fun close() {
        clear()
    }
}