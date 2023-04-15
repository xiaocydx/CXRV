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

package com.xiaocydx.cxrv.layout

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.xiaocydx.cxrv.itemvisible.findFirstCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.itemvisible.findFirstVisibleItemPosition
import com.xiaocydx.cxrv.itemvisible.findLastCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.itemvisible.findLastVisibleItemPosition
import java.lang.reflect.ParameterizedType

/**
 * [LayoutManager]扩展接口
 *
 * 在目录src\main\resources\META-INF\services下，
 * 创建文件com.xiaocydx.cxrv.layout.LayoutManagerExtensions，
 * 在文件中添加实现了[LayoutManagerExtensions]的类名（包含包名）。
 *
 * @author xcc
 * @date 2022/9/24
 */
interface LayoutManagerExtensions<T : LayoutManager> {
    /**
     * 需要匹配的[LayoutManager]类型
     */
    val layoutClass: Class<out T>
        get() {
            val type = javaClass.genericInterfaces.firstOrNull {
                if (it !is ParameterizedType) return@firstOrNull false
                it.rawType === LayoutManagerExtensions::class.java
            }
            @Suppress("UNCHECKED_CAST")
            val layoutClass = (type as? ParameterizedType)
                ?.actualTypeArguments?.get(0) as? Class<T>
            return requireNotNull(layoutClass) { "获取类型失败，请实现layoutClass属性" }
        }

    /**
     * 对应[RecyclerView.findFirstVisibleItemPosition]
     */
    fun findFirstVisibleItemPosition(layout: T): Int = NO_POSITION

    /**
     * 对应[RecyclerView.findFirstCompletelyVisibleItemPosition]
     */
    fun findFirstCompletelyVisibleItemPosition(layout: T): Int = NO_POSITION

    /**
     * 对应[RecyclerView.findLastVisibleItemPosition]
     */
    fun findLastVisibleItemPosition(layout: T): Int = NO_POSITION

    /**
     * 对应[RecyclerView.findLastCompletelyVisibleItemPosition]
     */
    fun findLastCompletelyVisibleItemPosition(layout: T): Int = NO_POSITION
}