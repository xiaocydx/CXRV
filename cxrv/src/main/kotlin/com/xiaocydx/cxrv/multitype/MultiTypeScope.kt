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

package com.xiaocydx.cxrv.multitype

import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 多类型作用域
 *
 * @author xcc
 * @date 2022/4/16
 */
class MultiTypeScope<T : Any> private constructor(
    private val delegate: MutableMultiTypeImpl<T>
) : MultiTypeAdapter<T>(), MutableMultiType<T> by delegate {

    /**
     * 列表适配器，仅用于作用域下构建初始化逻辑
     */
    @Deprecated(
        message = "已合并ListAdapter<T, *>和MutableMultiType<T>，" +
                "作用域下不再需要成员属性listAdapter构建初始化逻辑。",
        replaceWith = ReplaceWith("this")
    )
    val listAdapter: ListAdapter<T, *> = this

    @PublishedApi
    internal constructor() : this(MutableMultiTypeImpl())

    @PublishedApi
    internal fun complete(): ListAdapter<T, *> {
        delegate.complete()
        setMultiType(delegate)
        return this
    }
}