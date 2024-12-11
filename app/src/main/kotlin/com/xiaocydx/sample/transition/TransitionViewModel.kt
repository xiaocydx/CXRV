package com.xiaocydx.sample.transition

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * @author xcc
 * @date 2023/9/22
 */
class TransitionViewModel : ViewModel() {
    val rvId = ViewCompat.generateViewId()
    val state = flow {
        delay(LOADING_DURATION)
        emit(TransitionState.Content)
    }.stateIn(viewModelScope, Lazily, TransitionState.Loading)
}

const val LOADING_DURATION = 100L

enum class TransitionState {
    Loading, Content
}