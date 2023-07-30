package com.xiaocydx.sample.paging.complex

import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(
    private val token: Int,
    params: VideoStreamParams = VideoStreamHelper.consume(token),
    repository: ComplexRepository = ComplexRepository()
) : ViewModel() {
    private var position = params.position
    private val state = VideoStreamStateHolder(repository)
    val vpId = ViewCompat.generateViewId()
    val flow = state.flow(viewModelScope)
    val sharedName = params.sharedName

    init {
        state.sendEvent { VideoStreamHelper.send(token, it) }
        state.initState(params)
    }

    @CheckResult
    fun consumePosition() = position.also { position = NO_POSITION }

    fun selectVideo(position: Int) {
        state.selectVideo(position)
    }

    class Factory(private val token: Int) : ViewModelProvider.Factory {

        constructor(activity: VideoStreamActivity) : this(VideoStreamHelper.token(activity))

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === VideoStreamViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return VideoStreamViewModel(token) as T
        }
    }
}