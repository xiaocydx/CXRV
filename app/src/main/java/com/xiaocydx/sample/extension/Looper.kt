package com.xiaocydx.sample.extension

import android.os.Looper
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * `Looper.sThreadLocal`，用于临时替换线程的[Looper]对象
 */
private val looperThreadLocal: ThreadLocal<Looper>? = initializeLooperThreadLocalOrNull()

/**
 * 还未找到反射`Looper.sThreadLocal`失败的常规替代方案
 *
 * 有一种思路是反射访问当前线程的ThreadLocalMap，遍历ThreadLocalMap.Entry数组，
 * 用当前线程的Looper对象匹配`Entry.value`，匹配到的`Entry.key`就是Looper.sThreadLocal，
 * 但是Android禁止反射访问ThreadLocalMap的成员函数和成员属性，该思路无法作为常规替代方案。
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