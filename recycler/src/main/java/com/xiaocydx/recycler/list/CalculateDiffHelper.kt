package com.xiaocydx.recycler.list

import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.DiffUtil

/**
 * 计算两个列表的差异
 *
 * @author xcc
 * @date 2021/9/17
 */
@WorkerThread
internal fun <T> List<T>.calculateDiff(
    newList: List<T>,
    diffCallback: DiffUtil.ItemCallback<T>
): DiffUtil.DiffResult {
    val oldList = this
    return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return if (oldItem != null && newItem != null) {
                diffCallback.areItemsTheSame(oldItem, newItem)
            } else {
                oldItem == null && newItem == null
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                oldItem != null && newItem != null -> {
                    diffCallback.areContentsTheSame(oldItem, newItem)
                }
                oldItem == null && newItem == null -> true
                else -> {
                    // 仅当areItemsTheSame()返回true时，才会调用areContentsTheSame()，
                    // 若areContentsTheSame()执行到该分支，则表示出现断言异常。
                    throw AssertionError("areContentsTheSame()出现断言异常")
                }
            }
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return if (oldItem != null && newItem != null) {
                diffCallback.getChangePayload(oldItem, newItem)
            } else {
                // 仅当areItemsTheSame()返回true，areContentsTheSame()返回false时，
                // 才会调用getChangePayload()，若getChangePayload()执行到该分支，则表示出现断言异常。
                throw AssertionError("getChangePayload()出现断言异常")
            }
        }
    })
}