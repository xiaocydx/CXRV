package com.xiaocydx.cxrv.paging

/**
 * [PagingSource]的加载类型
 *
 * ### [LoadType]和[LoadParams]
 * [PagingSource.load]的加载参数通过[LoadParams.create]创建而来。
 *
 * ### [LoadType]和[LoadState]
 * [PagingEvent]包含[LoadType]和与之对应的[LoadState]，
 * 视图控制器接收[PagingEvent]，根据[LoadType]和[LoadState]更新加载视图。
 *
 * @author xcc
 * @date 2021/9/13
 */
enum class LoadType {
    /**
     * [PagingSource]的刷新加载
     *
     * 表示初始化加载或者主动触发的刷新加载
     */
    REFRESH,

    /**
     * [PagingSource]的末尾加载
     */
    APPEND
}