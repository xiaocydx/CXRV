package com.xiaocydx.sample.viewpager2.animatable

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.xiaocydx.cxrv.list.Disposable

/**
 * 添加受[RecyclerView]滚动控制的[AnimatableController]
 */
@Suppress("SpellCheckingInspection")
fun AnimatableMediator.controlledByScroll(): Disposable {
    findController(RecyclerViewScrollController::class.java)?.dispose()
    return RecyclerViewScrollController().attach(this)
}

private class RecyclerViewScrollController : OnScrollListener(), AnimatableController {
    private var mediator: AnimatableMediator? = null
    override val isDisposed: Boolean
        get() = mediator == null
    override val isAllowStart: Boolean
        get() = mediator != null && mediator!!.recyclerView.scrollState == SCROLL_STATE_IDLE

    fun attach(mediator: AnimatableMediator): Disposable {
        this.mediator = mediator
        mediator.also {
            it.addController(this)
            it.recyclerView.addOnScrollListener(this)
        }
        return this
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == SCROLL_STATE_IDLE) {
            mediator?.startAll()
        } else {
            mediator?.stopAll()
        }
    }

    override fun dispose() {
        mediator?.also {
            it.removeController(this)
            it.recyclerView.removeOnScrollListener(this)
        }
        mediator = null
    }
}