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

/**
 * 单个元素的多类型容器
 *
 * @author xcc
 * @date 2021/10/18
 */
@PublishedApi
internal class SingletonMultiType<T : Any>(
    clazz: Class<T>,
    delegate: ViewTypeDelegate<T, *>
) : MultiType<T> {
    private val type: Type<out T> = Type(clazz, delegate)

    override val size: Int = 1

    override fun keyAt(viewType: Int): Type<out T> = type

    override fun valueAt(index: Int): Type<out T> = type

    override fun itemAt(item: T): Type<out T> = type
}