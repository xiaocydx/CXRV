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

@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.cxrv.animatable

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.xiaocydx.cxrv.list.Disposable

/**
 * 添加受[lifecycle]控制的[AnimatableController]
 *
 * @param state 当[lifecycle]的状态小于[state]时，停止全部。
 */
fun AnimatableMediator.controlledByLifecycle(lifecycle: Lifecycle, state: Lifecycle.State): Disposable {
    findAnimatableController<RecyclerViewLifecycleController>()?.dispose()
    return RecyclerViewLifecycleController(state).attach(this, lifecycle)
}

private class RecyclerViewLifecycleController(
    private val state: Lifecycle.State
) : LifecycleEventObserver, AnimatableController {
    private var mediator: AnimatableMediator? = null
    private var lifecycle: Lifecycle? = null
    override val isDisposed: Boolean
        get() = mediator == null && lifecycle == null
    override val canStartAnimatable: Boolean
        get() = lifecycle != null && lifecycle!!.currentState.isAtLeast(state)

    fun attach(
        mediator: AnimatableMediator,
        lifecycle: Lifecycle
    ): Disposable {
        this.mediator = mediator
        this.lifecycle = lifecycle
        mediator.addAnimatableController(this)
        lifecycle.addObserver(this)
        return this
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        val startEvent = upTo(state)
        val stopEvent = downFrom(state)
        when (event) {
            ON_DESTROY -> dispose()
            startEvent -> mediator?.startAllAnimatable()
            stopEvent -> mediator?.stopAllAnimatable()
            else -> return
        }
    }

    private fun upTo(state: Lifecycle.State) = when (state) {
        CREATED -> ON_CREATE
        STARTED -> ON_START
        RESUMED -> ON_RESUME
        else -> null
    }

    private fun downFrom(state: Lifecycle.State) = when (state) {
        CREATED -> ON_DESTROY
        STARTED -> ON_STOP
        RESUMED -> ON_PAUSE
        else -> null
    }

    override fun dispose() {
        mediator?.removeAnimatableController(this)
        lifecycle?.removeObserver(this)
        mediator = null
        lifecycle = null
    }
}