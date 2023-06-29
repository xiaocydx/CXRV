package com.xiaocydx.sample.viewpager2.loop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.addItems
import com.xiaocydx.cxrv.list.asFlow
import com.xiaocydx.cxrv.list.size
import com.xiaocydx.cxrv.list.submitList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author xcc
 * @date 2023/5/11
 */
class ContentViewModel : ViewModel() {
    private val state = ListState<ContentItem>()
    private var pendingScrollToFirst = false
    val flow = state.asFlow()

    init {
        refresh(timeMillis = 0)
    }

    fun refresh(timeMillis: Long) {
        viewModelScope.launch {
            delay(timeMillis)
            pendingScrollToFirst = true
            state.submitList((1..3).map { ContentItem(it, "Page$it") })
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

    fun consumeScrollToFirst() = pendingScrollToFirst.also { pendingScrollToFirst = false }
}