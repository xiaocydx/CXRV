package com.xiaocydx.sample.paging.complex

import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingSource

interface PagingInterceptor<K : Any, T : Any> {
    fun intercept(source: PagingSource<K, T>): PagingSource<K, T>
}

class PagingSyncHelper<K : Any, T : Any> : PagingInterceptor<K, T> {
    private var isRefreshClear = false
    private var recordNextKey: K? = null
    private var pendingResult: LoadResult<K, T>? = null
    val nextKey: K?
        get() {
            val pendingResult = pendingResult
            return if (pendingResult is LoadResult.Success) pendingResult.nextKey else recordNextKey
        }

    fun refresh(pager: Pager<K, T>, data: List<T>, nextKey: K?) {
        isRefreshClear = false
        pendingResult = LoadResult.Success(data, nextKey)
        pager.refresh()
    }

    fun append(pager: Pager<K, T>, data: List<T>, nextKey: K?) {
        if (this.nextKey == nextKey) return
        val pendingResult = pendingResult
        if (pendingResult is LoadResult.Success) {
            this.pendingResult = LoadResult.Success(pendingResult.data + data, nextKey)
        } else {
            this.pendingResult = LoadResult.Success(data, nextKey)
        }
        pager.append()
    }

    override fun intercept(source: PagingSource<K, T>): PagingSource<K, T> = PagingSourceImpl(source)

    private inner class PagingSourceImpl(private val source: PagingSource<K, T>) : PagingSource<K, T> {

        override suspend fun load(params: LoadParams<K>): LoadResult<K, T> {
            if (params is LoadParams.Refresh) {
                if (isRefreshClear) pendingResult = null
                isRefreshClear = true
            }
            val result = if (pendingResult != null) {
                pendingResult!!.also { pendingResult = null }
            } else {
                source.load(params)
            }
            if (result is LoadResult.Success) {
                recordNextKey = result.nextKey
            }
            return result
        }
    }
}