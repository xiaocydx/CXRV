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

package com.xiaocydx.cxrv.viewpager2.loop

import androidx.recyclerview.widget.LoopPagerScroller
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2

/**
 * [ViewPager2]循环页面的锚点信息更新器，[LoopPagerScroller]是唯一实现类
 *
 * @author xcc
 * @date 2023/5/13
 */
internal interface LoopAnchorUpdater {

    /**
     * 若`viewPager.currentItem`是附加页面，则更新锚点信息
     *
     * 以A和B是原始页面，`B*`和`A*`是附加页面为例：
     * ```
     * {B* ，A ，B ，A*}
     * ```
     * 假设`viewPager.currentItem`为`B*`，当开始滚动`B*`时，
     * 会更新锚点信息，下一帧以`B`为锚点，往两侧填充子View。
     *
     * 实现类会优化更新锚点信息的过程，避免产生其他影响，
     * 优化效果可以理解为将`B*`的`itemView`，挪到`B`处，
     * `itemView`不会被移除，也不会绑定新的[ViewHolder]。
     */
    fun updateAnchorForCurrent()

    /**
     * 若`viewPager.currentItem`是附加页面，则更新锚点信息
     *
     * **注意**：该函数仅支持一帧内一次插入更新的场景，且没有[updateAnchorForCurrent]的优化效果。
     *
     * @param lastContentCount 之前的内容`item`数量
     * @param positionStart    插入的`bindingAdapterPosition`
     * @param itemCount        插入的`item`数量
     */
    fun updateAnchorForInserted(lastContentCount: Int, positionStart: Int, itemCount: Int)
}