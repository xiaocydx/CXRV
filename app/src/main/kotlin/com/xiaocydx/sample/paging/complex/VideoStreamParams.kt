package com.xiaocydx.sample.paging.complex

import androidx.recyclerview.widget.RecyclerView.NO_POSITION

data class VideoStreamParams(
    val data: List<ComplexItem> = emptyList(),
    val nextKey: Int? = null,
    val position: Int = NO_POSITION
)

sealed class VideoStreamEvent {
    class Select(val id: String) : VideoStreamEvent()
    class Refresh(val data: List<ComplexItem>, val nextKey: Int?) : VideoStreamEvent()
    class Append(val data: List<ComplexItem>, val nextKey: Int?) : VideoStreamEvent()
}