package com.xiaocydx.sample.viewpager2.loop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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

    val flow = state.asFlow()
    val refreshEvent = _refreshEvent.asSharedFlow()

    init {
        refresh(timeMillis = 0)
    }

    fun refresh(timeMillis: Long) {
        viewModelScope.launch {
            delay(timeMillis)
            state.submitList((1..3).map { ContentItem(it, "Page$it") })
            _refreshEvent.tryEmit(Unit)
        }
    }

    fun append(timeMillis: Long) {
        viewModelScope.launch {
            delay(timeMillis)
            val size = state.size
            val start = size + 1
            val end = start + 2
            state.addItems(size, (start..end).map { ContentItem(it, "Page$it") })
        }
    }
}