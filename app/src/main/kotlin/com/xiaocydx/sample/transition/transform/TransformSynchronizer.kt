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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * [TransformSender]的同步Id
 *
 * [TransformSenderId.asPosition]可以转换为[TransformSenderPosition]。
 */
interface TransformSenderId {

    /**
     * 当[TransformReceiver]改变位置时，发射[TransformSender]的同步Id
     */
    val syncEvent: Flow<String>

    /**
     * 当[TransformReceiver]退出时，[TransformSender]需要消费同步Id，
     * [TransformSender]可以收集[TransformSender.transformReturn]消费同步Id。
     *
     * @return 返回`null`，表示没有同步Id或者已消费同步Id。
     */
    fun consume(): String?
}

/**
 * [TransformSender]的同步位置
 */
interface TransformSenderPosition {

    /**
     * 当[TransformReceiver]改变位置时，发射[TransformSender]的同步位置，
     * [TransformSender]需要收集[syncEvent]，将当前列表滚动到同步位置。
     *
     * [TransformSender.launchTransformSync]演示了如何收集[syncEvent]。
     */
    val syncEvent: Flow<Int>

    /**
     * 当[TransformReceiver]退出时，[TransformSender]需要消费同步位置，
     * [TransformSender]可以收集[TransformSender.transformReturn]消费同步位置。
     *
     * [TransformSender.launchTransformSync]演示了如何消费同步位置。
     *
     * @return 返回`-1`，表示没有同步位置或者已消费同步位置。
     */
    fun consume(): Int
}

/**
 * 将[TransformSenderId]转换为[TransformSenderPosition]
 */
inline fun TransformSenderId.asPosition(
    crossinline position: (key: String) -> Int
): TransformSenderPosition = object : TransformSenderPosition {
    override val syncEvent: Flow<Int> = this@asPosition.syncEvent
        .map { position(it) }.filter { it != -1 }

    override fun consume(): Int = this@asPosition.consume()?.let(position) ?: -1
}

/**
 * 将[TransformSenderId]转换为[TransformSenderPosition]
 */
inline fun <T : Any> TransformSenderId.asPosition(
    crossinline list: () -> List<T>,
    crossinline key: (item: T) -> String
) = asPosition { list().indexOfFirst { item -> key(item) == it } }

/**
 * [TransformSender]的同步Id，需要在[ViewModel]中创建并保留
 *
 * 实际场景决定是否通过[SavedStateHandle]记录[TransformSender]和[TransformReceiver]的Key，
 * 对于分页列表场景，如果没有持久化列表数据，那么通过[SavedStateHandle]记录Key的意义并不大，
 * 因为缺少Key对应位置的列表数据，无法正确恢复位置。
 */
class SenderId {
    private var syncKey: String? = null
    private val _syncEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val syncEvent = _syncEvent.onStart { syncKey?.let { emit(it) } }
    private fun consume() = syncKey.also { syncKey = null }

    /**
     * 发射[TransformSender]的同步[id]
     */
    fun sync(id: String) {
        if (syncKey == id) return
        syncKey = id
        _syncEvent.tryEmit(id)
    }

    /**
     * 记录[TransformSender]的同步[id]
     */
    fun record(id: String) {
        syncKey = id
    }

    /**
     * 将[SenderId]转换为[TransformSender]可处理的[TransformSenderId]
     */
    fun asTransform(): TransformSenderId {
        return object : TransformSenderId {
            override val syncEvent = this@SenderId.syncEvent
            override fun consume() = this@SenderId.consume()
        }
    }
}