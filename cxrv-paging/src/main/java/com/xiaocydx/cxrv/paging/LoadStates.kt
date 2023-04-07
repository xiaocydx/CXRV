package com.xiaocydx.cxrv.paging

import androidx.annotation.CheckResult

/**
 * [PagingSource]的加载状态集合
 *
 * @author xcc
 * @date 2021/9/13
 */
data class LoadStates(
    /**
     * 对应[LoadType.REFRESH]的加载状态
     */
    val refresh: LoadState,

    /**
     * 对应[LoadType.APPEND]的加载状态
     */
    val append: LoadState
) {

    companion object {
        /**
         * 加载未完成状态集合，用于表示初始化状态
         */
        val Incomplete = LoadStates(
            refresh = LoadState.Incomplete,
            append = LoadState.Incomplete
        )
    }
}

/**
 * 是否刷新或者末尾加载中
 */
val LoadStates.isLoading: Boolean
    get() = refresh.isLoading || append.isLoading

/**
 * 是否刷新或者末尾加载失败
 */
val LoadStates.isFailure: Boolean
    get() = refresh.isFailure || append.isFailure

/**
 * 是否刷新或者末尾加载完全，没有更多数据
 */
val LoadStates.isFully: Boolean
    get() = refresh.isFully || append.isFully

/**
 * 是否允许末尾加载
 */
val LoadStates.isAllowAppend: Boolean
    get() = when {
        !refresh.isSuccess || refresh.isFully -> false
        append.isLoading || append.isFully -> false
        else -> true
    }

/**
 * 刷新或者末尾加载失败的异常，返回`null`表示当前没有加载失败
 */
val LoadStates.exception: Throwable?
    get() = refresh.exception ?: append.exception

/**
 * 加载失败的加载类型，返回`null`表示当前没有加载失败
 */
val LoadStates.failureLoadType: LoadType?
    get() = when {
        refresh.isFailure -> LoadType.REFRESH
        append.isFailure -> LoadType.APPEND
        else -> null
    }

/**
 * 修改[loadType]的状态，创建新的状态集合
 */
@CheckResult
fun LoadStates.modifyState(
    loadType: LoadType,
    newState: LoadState
): LoadStates = when (loadType) {
    LoadType.REFRESH -> copy(refresh = newState)
    LoadType.APPEND -> copy(append = newState)
}

/**
 * 获取[loadType]的状态
 */
fun LoadStates.getState(
    loadType: LoadType
): LoadState = when (loadType) {
    LoadType.REFRESH -> refresh
    LoadType.APPEND -> append
}

/**
 * 当前状态集合和[states]对比，是否为刷新加载成功的流转过程
 */
fun LoadStates.refreshToSuccess(states: LoadStates): Boolean = when {
    !this.append.isIncomplete || !states.append.isIncomplete -> false
    else -> !this.refresh.isSuccess && states.refresh.isSuccess
}

/**
 * 当前状态集合和[states]对比，是否为刷新加载完成的流转过程
 *
 * **注意**：加载成功或加载失败即算加载完成。
 */
fun LoadStates.refreshToComplete(states: LoadStates): Boolean = when {
    !this.append.isIncomplete || !states.append.isIncomplete -> false
    else -> !this.refresh.isComplete && states.refresh.isComplete
}

/**
 * 当前状态集合和[states]对比，是否为末尾加载完全的流转过程
 */
fun LoadStates.appendToFully(states: LoadStates): Boolean = when {
    !this.refresh.isSuccess || !states.refresh.isSuccess -> false
    else -> !this.append.isFully && states.append.isFully
}