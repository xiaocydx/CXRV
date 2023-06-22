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
 * 未完成多类型注册的标识
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> unregistered(): MutableMultiType<T> {
    return Unregistered as MutableMultiType<T>
}

private object Unregistered : MutableMultiType<Any>() {
    override val size: Int
        get() = error()

    override fun keyAt(viewType: Int) = error()

    override fun valueAt(index: Int) = error()

    override fun itemAt(item: Any) = error()

    override fun register(type: Type<out Any>) = error()

    private fun error(): Nothing {
        throw IllegalStateException("未完成多类型注册")
    }
}