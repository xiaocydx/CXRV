package com.xiaocydx.cxrv.list

/**
 * 提供列表状态相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2022/3/8
 */
internal interface ListMediator<T : Any> {
    /**
     * 列表版本号
     *
     * 若[updateList]更新成功，则增加版本号
     */
    val version: Int

    /**
     * 当前列表
     */
    val currentList: List<T>

    /**
     * 执行列表更新操作
     */
    fun updateList(op: UpdateOp<T>)
}