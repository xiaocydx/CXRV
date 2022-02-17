package com.xiaocydx.recycler

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

fun testMainDispatcher(): MainCoroutineDispatcher = TestMainDispatcher()

fun testWorkDispatcher(dispatchDelay: Long = 0): CoroutineDispatcher {
    return Executors.newCachedThreadPool { runnable ->
        thread(start = false, isDaemon = true, name = "test work") {
            if (dispatchDelay > 0) {
                Thread.sleep(dispatchDelay)
            }
            runnable.run()
        }
    }.asCoroutineDispatcher()
}

private class TestMainDispatcher : MainCoroutineDispatcher() {
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        thread(start = false, isDaemon = true, name = "test main") {
            runnable.run()
        }
    }.asCoroutineDispatcher()

    override val immediate: MainCoroutineDispatcher = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread().name != "test main"
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatcher.dispatch(context, block)
    }
}