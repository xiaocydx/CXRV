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

import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.viewpager2.widget.ViewPager2

/**
 * [ViewPager2]循环页面的内容，提供`position`的计算函数
 *
 * 以A和B是原始页面，`B*`和`A*`是额外页面为例：
 * ```
 * {B* ，A ，B ，A*}
 * ```
 *
 * 此时各属性的数值：
 * * [itemCount] = 2
 * * [supportLoop] = true
 *
 * * layoutPosition = `{0 ，1 ，2 ，3}`
 * * [layoutPositionRange] = `[0, 3]`
 * * [startExtraLayoutPositionRange] = `[0, 0]`
 * * [endExtraLayoutPositionRange] = `[3, 3]`
 *
 * * bindingAdapterPosition = `{1 ，0 ，1 ，0}`
 * * [bindingAdapterPositionRange] = `[0, 1]`
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerContent(
    val viewPager2: ViewPager2,
    val adapter: Adapter<*>,
    val extraPageLimit: Int
) {

    /**
     * 内容`item`数量
     */
    val itemCount: Int
        get() = adapter.itemCount

    /**
     * 是否支持循环
     */
    val supportLoop: Boolean
        get() = itemCount > 1

    /**
     * 将`bindingAdapterPosition`转换为`layoutPosition`
     *
     * @param bindingAdapterPosition 取值范围是[bindingAdapterPositionRange]。
     * @return 当[bindingAdapterPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toLayoutPosition(bindingAdapterPosition: Int): Int {
        if (bindingAdapterPosition !in bindingAdapterPositionRange()) return NO_POSITION
        var layoutPosition = bindingAdapterPosition
        if (supportLoop) layoutPosition += extraPageLimit
        return layoutPosition
    }

    /**
     * 将`layoutPosition`转换为`bindingAdapterPosition`
     *
     * @param layoutPosition 取值范围是[layoutPositionRange]。
     * @return 当[layoutPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toBindingAdapterPosition(layoutPosition: Int): Int {
        if (layoutPosition !in layoutPositionRange()) return NO_POSITION
        if (!supportLoop) return layoutPosition
        var bindingAdapterPosition = (layoutPosition - extraPageLimit) % itemCount
        if (bindingAdapterPosition < 0) bindingAdapterPosition += itemCount
        return bindingAdapterPosition
    }

    /**
     * `layoutPosition`的取值范围
     *
     * @param contentCount 默认按当前`adapter.itemCount`计算取值范围
     */
    fun layoutPositionRange(contentCount: Int = itemCount): IntRange {
        val extraCount = if (!supportLoop) 0 else extraPageLimit * 2
        return 0 until contentCount + extraCount
    }

    /**
     * 起始端额外页面的`layoutPosition`取值范围
     *
     * @param contentCount 默认按当前`adapter.itemCount`计算取值范围
     */
    fun startExtraLayoutPositionRange(contentCount: Int = itemCount): IntRange {
        val extraCount = if (!supportLoop) 0 else extraPageLimit
        return 0 until extraCount
    }

    /**
     * 结束端额外页面的`layoutPosition`取值范围
     *
     * @param contentCount 默认按当前`adapter.itemCount`计算取值范围
     */
    fun endExtraLayoutPositionRange(contentCount: Int = itemCount): IntRange {
        val extraCount = if (!supportLoop) 0 else extraPageLimit
        val first = extraCount + contentCount
        return first until first + extraCount
    }

    /**
     * `bindingAdapterPosition`的取值范围
     *
     * @param contentCount 默认按当前`adapter.itemCount`计算取值范围
     */
    fun bindingAdapterPositionRange(contentCount: Int = itemCount): IntRange {
        return 0 until contentCount
    }
}