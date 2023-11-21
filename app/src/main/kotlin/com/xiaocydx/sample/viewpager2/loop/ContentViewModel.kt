package com.xiaocydx.sample.viewpager2.loop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author xcc
 * @date 2023/5/11
 */
class ContentViewModel : ViewModel() {
    private val list = MutableStateList<ContentItem>()
    private var pendingScrollToFirst = false
    val flow = list.asStateFlow()

    init {
        refresh(timeMillis = 0)
    }

    fun refresh(timeMillis: Long) {
        viewModelScope.launch {
            delay(timeMillis)
            pendingScrollToFirst = true
            list.submit((1..3).map { ContentItem(it, "Page$it") })
        }
    }

    fun append(timeMillis: Long) {
        viewModelScope.launch {
            delay(timeMillis)
            val size = list.size
            val start = size + 1
            val end = start + 2
            list.addAll((start..end).map { ContentItem(it, "Page$it") })
        }
    }

    fun consumeScrollToFirst() = pendingScrollToFirst.also { pendingScrollToFirst = false }
}