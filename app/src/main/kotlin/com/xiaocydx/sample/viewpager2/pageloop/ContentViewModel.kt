package com.xiaocydx.sample.viewpager2.pageloop

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import com.xiaocydx.cxrv.list.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/5/11
 */
class ContentViewModel : ViewModel() {
    private val state = ListState<ContentItem>()
    private val _refreshEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val vp2Id = ViewCompat.generateViewId()
    val flow = state.asFlow()
    val refreshEvent = _refreshEvent.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        state.submitList((1..6).map { ContentItem(it, "Page$it") })
        _refreshEvent.tryEmit(Unit)
    }

    fun append() {
        val size = state.size
        val start = size + 1
        val end = start + 2
        state.addItems(size, (start..end).map { ContentItem(it, "Page$it") })
    }
}