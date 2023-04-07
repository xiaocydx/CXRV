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

package com.xiaocydx.sample.extension

import android.os.Looper
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * `Looper.sThreadLocal`，用于临时替换线程的[Looper]对象
 */
private val looperThreadLocal: ThreadLocal<Looper>? = initializeLooperThreadLocalOrNull()

/**
 * 若后续Android版本反射`sThreadLocal`失败，则可以考虑使用：
 * [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
 */
private fun initializeLooperThreadLocalOrNull(): ThreadLocal<Looper>? = try {
    @Suppress("DiscouragedPrivateApi")
    val field = Looper::class.java.getDeclaredField("sThreadLocal")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    field.get(null) as? ThreadLocal<Looper>
} catch (e: Throwable) {
    null
}

/**
 * 协程的[Looper]上下文元素，用于临时替换协程所处线程的[Looper]对象
 */
internal class LooperElement(private val newState: Looper) : ThreadContextElement<Looper?> {
    companion object Key : CoroutineContext.Key<LooperElement>

    override val key: CoroutineContext.Key<*> = Key

    /**
     * 启动或恢复协程时被调用，修改协程所处线程的[Looper]对象
     */
    override fun updateThreadContext(context: CoroutineContext): Looper? {
        val oldState = Looper.myLooper()
        if (oldState !== newState) looperThreadLocal?.set(newState)
        return oldState
    }

    /**
     * 完成或挂起协程时被调用，恢复协程所处线程的[Looper]对象
     */
    override fun restoreThreadContext(context: CoroutineContext, oldState: Looper?) {
        if (oldState !== Looper.myLooper()) looperThreadLocal?.set(oldState)
    }
}