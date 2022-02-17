package com.xiaocydx.recycler.list

/**
 * 执行列表更新操作的监听
 *
 * @author xcc
 * @date 2021/11/29
 */
fun interface ListExecuteListener<in T : Any> {

    /**
     * 开始执行列表更新操作
     */
    fun onExecute(op: UpdateOp<T>)
}