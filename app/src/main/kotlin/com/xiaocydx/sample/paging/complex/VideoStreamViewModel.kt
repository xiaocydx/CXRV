package com.xiaocydx.sample.paging.complex

import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * // TODO: 2023/8/2 简化示例代码
 *
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(
    private val complexViewModel: ComplexListViewModel,
    repository: ComplexRepository = ComplexRepository()
) : ViewModel() {
    private var position = NO_POSITION
    private val state = VideoStreamStateHolder(repository)
    val vpId = ViewCompat.generateViewId()
    val flow = state.flow(viewModelScope)

    init {
        val params = complexViewModel.consumeParams()
        position = params.position
        state.sendEvent(complexViewModel::sync)
        state.initState(params)
    }

    @CheckResult
    fun consumePosition() = position.also { position = NO_POSITION }

    fun selectVideo(position: Int) {
        state.selectVideo(position)
    }

    class Factory(private val viewModel: ComplexListViewModel) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === VideoStreamViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return VideoStreamViewModel(viewModel) as T
        }
    }
}