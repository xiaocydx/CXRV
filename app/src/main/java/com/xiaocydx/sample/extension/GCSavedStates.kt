package com.xiaocydx.sample.extension

import androidx.collection.LongSparseArray
import androidx.collection.keyIterator
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.reflect.Field

/**
 * 清除`mSavedStates`保存的无效Fragment状态
 *
 * [FragmentStateAdapter]销毁Fragment时，
 * 若还包含item，则会将Fragment状态保存到成员属性`mSavedStates`，
 * 后面滚动回item重新创建Fragment时，通过`mSavedStates`保存的Fragment状态恢复视图状态。
 * 但是存在一个逻辑漏洞，若保存Fragment状态后，不再滚动回item，
 * 而是移除item（例如整体刷新），则`mSavedStates`保存的Fragment状态不会被清除。
 * 对于频繁移除item并且是常驻首页的ViewPager2页面来说，
 * 这个逻辑漏洞可能会导致无效Fragment状态堆积，需要考虑在合适的时机，
 * 通过反射清除`mSavedStates`保存的无效Fragment状态。
 */
fun FragmentStateAdapter.gcSavedStates() {
    val mSavedStatesField: Field = runCatching {
        FragmentStateAdapter::class.java.getDeclaredField("mSavedStates")
    }.getOrNull() ?: return

    mSavedStatesField.isAccessible = true
    val mSavedStates = mSavedStatesField.get(this) as? LongSparseArray<*> ?: return

    for (itemId in mSavedStates.keys()) {
        if (!containsItem(itemId)) mSavedStates.remove(itemId)
    }
}

private fun LongSparseArray<*>.keys(): LongArray {
    var index = 0
    val keys = LongArray(size())
    for (key in keyIterator()) {
        keys[index] = key
        index++
    }
    return keys
}