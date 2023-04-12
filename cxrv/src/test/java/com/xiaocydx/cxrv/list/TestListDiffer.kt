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

package com.xiaocydx.cxrv.list

import android.os.Looper
import kotlinx.coroutines.*
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * @param dispatchDelay 调度延时，用于模拟较长时间的差异计算
 */
@Suppress("TestFunctionName")
internal fun TestDiffDispatcher(dispatchDelay: Long = 0): CoroutineDispatcher {
    val executor = object : ThreadPoolExecutor(
        /* corePoolSize */0,
        /* maximumPoolSize */Int.MAX_VALUE,
        /* keepAliveTime */60L,
        /* unit */TimeUnit.SECONDS,
        /* workQueue */SynchronousQueue(),
        /* threadFactory */{ runnable ->
            thread(start = false, isDaemon = true, block = { runnable.run() })
        }
    ) {
        override fun beforeExecute(t: Thread, r: Runnable) {
            if (dispatchDelay > 0) Thread.sleep(dispatchDelay)
        }
    }
    return executor.asCoroutineDispatcher()
}

/**
 * [runBlocking]产生挂起会休眠主线程，不同于Android消息队列的`epoll_wait()`，
 * [CoroutineListDiffer]通过Android平台的[Dispatchers.Main]无法唤醒主线程，
 * 解决办法是将[BlockingEventLoop]包装为[MainCoroutineDispatcher]，
 * 让[CoroutineListDiffer]通过[TestMainDispatcher]唤醒主线程。
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
internal class TestMainDispatcher(private val eventLoop: BlockingEventLoop) : MainCoroutineDispatcher() {
    private val thread = eventLoop.thread()
    override val immediate: MainCoroutineDispatcher = this

    constructor(context: CoroutineContext) : this(kotlin.run {
        val eventLoop = context[ContinuationInterceptor] as? BlockingEventLoop
        assert(eventLoop?.thread() === Looper.getMainLooper().thread)
        eventLoop!!
    })

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() !== thread
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        eventLoop.dispatch(context, block)
    }
}

/**
 * 直接访问成员属性`thread`会编译报错，通过反射访问
 */
@Suppress("INVISIBLE_REFERENCE")
private fun BlockingEventLoop.thread(): Thread {
    val method = javaClass.getDeclaredMethod("getThread")
    method.isAccessible = true
    return method.invoke(this) as Thread
}