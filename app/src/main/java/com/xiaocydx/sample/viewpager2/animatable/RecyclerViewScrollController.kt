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