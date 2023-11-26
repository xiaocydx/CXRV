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

package com.xiaocydx.sample.transition.transform

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile

/**
 * 同步[TransformSender]的Id
 *
 * 1. 当[TransformReceiver]滚动时，[syncEvent]发射Id进行同步。
 * 2. 当[TransformReceiver]退出时，需要调用[consume]消费Id进行同步。
 * 3. [TransformSenderId.asPosition]可以将Id转换为Position进行同步，
 * [TransformSender.launchSenderSync]演示了如何消费Position。
 */
@MainThread
interface TransformSenderId : TransformKey<String?> {
    override val syncEvent: Flow<String>
    override fun consume(): String?
}

@MainThread
interface TransformSenderPosition : TransformKey<Int>

/**
 * 将Id转换为Position进行同步
 */
inline fun <T : Any> TransformSenderId.asPosition(
    crossinline id: (item: T) -> String,
    crossinline list: () -> List<T>
) = asPosition { list().indexOfFirst { item -> id(item) == it } }

/**
 * 将Id转换为Position进行同步
 */
inline fun TransformSenderId.asPosition(
    crossinline position: (key: String) -> Int
): TransformSenderPosition = object : TransformSenderPosition {
    override val syncEvent: Flow<Int> = this@asPosition.syncEvent
        .filterNotNull().map { position(it) }.filter { it != -1 }

    override fun consume(): Int = this@asPosition.consume()?.let(position) ?: -1
}

@MainThread
interface TransformKey<T> {
    val syncEvent: Flow<T>
    fun consume(): T
}

/**
 * 同步[TransformSender]的Id，可以在[ViewModel]中创建并保留
 *
 * 实际场景决定是否通过[SavedStateHandle]记录[TransformSender]和[TransformReceiver]的Id，
 * 对于分页列表场景，如果没有持久化列表数据，那么通过[SavedStateHandle]记录Id的意义并不大，
 * 因为缺少Id对应Position的列表数据，无法正确恢复Position。
 */
@MainThread
class SyncTransformSenderId : TransformSenderId {
    private var isCancelled = false
    private var recordId: String? = null
    private val _syncEvent = MutableSharedFlow<Any>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Suppress("UNCHECKED_CAST")
    override val syncEvent = _syncEvent
        .onStart { if (isCancelled) emit(CancelValue) else recordId?.let { emit(it) } }
        .takeWhile { it != CancelValue }.flowOn(Dispatchers.Main.immediate) as Flow<String>

    override fun consume(): String? {
        assertMainThread()
        return recordId.also { recordId = null }
    }

    /**
     * 发射[id]进行同步
     */
    fun sync(id: String) {
        assertMainThread()
        if (isCancelled || recordId == id) return
        recordId = id
        _syncEvent.tryEmit(id)
    }

    /**
     * 记录[id]，用于[consume]
     */
    fun record(id: String) {
        assertMainThread()
        if (isCancelled) return
        recordId = id
    }

    /**
     * 停止发射id，清除记录的id
     */
    fun close() {
        assertMainThread()
        isCancelled = true
        recordId = null
        _syncEvent.tryEmit(CancelValue)
    }

    fun asSenderId(): TransformSenderId {
        return object : TransformSenderId {
            override val syncEvent = this@SyncTransformSenderId.syncEvent
            override fun consume() = this@SyncTransformSenderId.consume()
        }
    }

    private fun assertMainThread() {
        assert(Thread.currentThread() === Looper.getMainLooper().thread)
    }

    private companion object CancelValue
}