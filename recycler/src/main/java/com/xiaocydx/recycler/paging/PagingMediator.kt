package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.UpdateOp

/**
 * 提供分页相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2021/9/15
 */
internal interface PagingMediator<T : Any> {

    /**
     * 列表更新版本号
     */
    val updateVersion: Int

    /**
     * 加载状态集合
     */
    val loadStates: LoadStates

    /**
     * 当前列表
     */
    val currentList: List<T>

    /**
     * 刷新加载，获取新的[PagingData]
     */
    fun refresh()

    /**
     * 末尾加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun append()

    /**
     * 重新加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun retry()

    /**
     * 更新列表
     */
    fun updateList(op: UpdateOp<T>)
}