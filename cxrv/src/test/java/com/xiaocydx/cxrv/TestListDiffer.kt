package com.xiaocydx.cxrv

import com.xiaocydx.cxrv.list.TestMainCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

fun testMainDispatcher(): MainCoroutineDispatcher = TestMainDispatcher()

fun testWorkDispatcher(dispatchDelay: Long = 0): CoroutineDispatcher {
    return TestWorkExecutor(dispatchDelay).asCoroutineDispatcher()
}

private class TestWorkExecutor(
    private val dispatchDelay: Long
) : ThreadPoolExecutor(
    0, Int.MAX_VALUE,
    60L, TimeUnit.SECONDS,
    SynchronousQueue(),
    { runnable ->
        thread(start = false, isDaemon = true) { runnable.run() }
    }
) {
    override fun beforeExecute(t: Thread?, r: java.lang.Runnable?) {
        if (dispatchDelay > 0) Thread.sleep(dispatchDelay)
    }
}

private class TestMainDispatcher : TestMainCoroutineDispatcher() {
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        thread(start = false, isDaemon = true) {
            isTestMain.set(true)
            runnable.run()
        }
    }.asCoroutineDispatcher()

    override val immediate: MainCoroutineDispatcher = this

    /**
     * 不能根据[Thread.currentThread.name]判断是否为测试主线程，会有额外后缀。
     */
    @Suppress("KDocUnresolvedReference")
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !isTestMain.get()!!
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatcher.dispatch(context, block)
    }

    private companion object {
        val isTestMain = object : ThreadLocal<Boolean>() {
            override fun initialValue(): Boolean = false
        }
    }
}