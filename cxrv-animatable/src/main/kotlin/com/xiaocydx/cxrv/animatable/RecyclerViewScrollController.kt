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

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.xiaocydx.cxrv.list.Disposable

/**
 * 添加受[AnimatableMediator.recyclerView]滚动控制的[AnimatableController]
 */
fun AnimatableMediator.controlledByScroll(): Disposable {
    findAnimatableController<RecyclerViewScrollController>()?.dispose()
    return RecyclerViewScrollController().attach(this)
}

private class RecyclerViewScrollController : OnScrollListener(), AnimatableController {
    private var mediator: AnimatableMediator? = null
    override val isDisposed: Boolean
        get() = mediator == null
    override val canStartAnimatable: Boolean
        get() = mediator != null && mediator!!.recyclerView.scrollState == SCROLL_STATE_IDLE

    fun attach(mediator: AnimatableMediator): Disposable {
        this.mediator = mediator
        mediator.also {
            it.addAnimatableController(this)
            it.recyclerView.addOnScrollListener(this)
        }
        return this
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == SCROLL_STATE_IDLE) {
            mediator?.startAllAnimatable()
        } else {
            mediator?.stopAllAnimatable()
        }
    }

    override fun dispose() {
        mediator?.also {
            it.removeAnimatableController(this)
            it.recyclerView.removeOnScrollListener(this)
        }
        mediator = null
    }
}