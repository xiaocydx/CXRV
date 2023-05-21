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

package com.xiaocydx.cxrv.viewpager2.fragment

import androidx.collection.LongSparseArray
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

/**
 * ```
 * public abstract class FragmentStateAdapter extends
 *         RecyclerView.Adapter<FragmentViewHolder> implements StatefulAdapter {
 *     private final LongSparseArray<Fragment.SavedState> mSavedStates = new LongSparseArray<>();
 * }
 * ```
 */
private val mSavedStatesField = runCatching {
    FragmentStateAdapter::class.java.getDeclaredField("mSavedStates")
}.getOrNull()?.apply { isAccessible = true }

/**
 * 清除`FragmentStateAdapter.mSavedStates`保存的无效Fragment状态
 *
 * 当[FragmentStateAdapter]销毁Fragment时，
 * 若还包含item，则会将Fragment状态保存到成员属性`mSavedStates`，
 * 当滚动回item重新创建Fragment时，通过`mSavedStates`恢复视图状态。
 * 但这个过程存在一个逻辑漏洞，若保存Fragment状态后，不再滚动回item，
 * 而是移除item（例如整体刷新），则`mSavedStates`保存的Fragment状态不会被清除。
 * 对频繁移除item并且是常驻应用首页的[ViewPager2]页面来说，可能会堆积无效Fragment状态，
 * 需要考虑在合适的时机调用该函数，通过反射清除`mSavedStates`保存的无效Fragment状态。
 */
fun FragmentStateAdapter.gcSavedStates() {
    val mSavedStates = mSavedStatesField?.get(this) as? LongSparseArray<*> ?: return
    for (itemId in mSavedStates.keys()) {
        if (!containsItem(itemId)) mSavedStates.remove(itemId)
    }
}

private fun LongSparseArray<*>.keys(): LongArray {
    val keys = LongArray(size())
    for (index in 0 until size()) {
        keys[index] = keyAt(index)
    }
    return keys
}