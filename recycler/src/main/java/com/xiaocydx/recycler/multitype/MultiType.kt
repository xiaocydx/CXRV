package com.xiaocydx.recycler.multitype

import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * 不可变的多类型容器
 *
 * @author xcc
 * @date 2021/10/9
 */
interface MultiType<T : Any> {

    /**
     * 类型数量
     */
    val size: Int

    /**
     * 返回指定[viewType]的类型
     */
    fun keyAt(viewType: Int): Type<out T>?

    /**
     * 返回指定[index]的类型
     */
    fun valueAt(index: Int): Type<out T>

    /**
     * 返回指定[item]的类型
     */
    fun itemAt(item: T): Type<out T>?
}

/**
 * 访问每个类型并执行指定操作
 */
inline fun <T : Any> MultiType<T>.forEach(action: (Type<out T>) -> Unit) {
    for (index in 0 until size) {
        action(valueAt(index))
    }
}

/**
 * 获取[item]对应的ViewType
 */
@Throws(IllegalArgumentException::class)
fun <T : Any> MultiType<T>.getItemViewType(item: T): Int {
    val type = requireNotNull(itemAt(item)) {
        "获取ViewType失败，请确认是否未注册class = ${item.javaClass.canonicalName}的ViewTypeDelegate。"
    }
    return type.delegate.viewType
}

/**
 * 获取[holder.itemViewType]对应的[ViewTypeDelegate]
 */
@Suppress("KDocUnresolvedReference")
@Throws(IllegalArgumentException::class)
fun <T : Any> MultiType<T>.getViewTypeDelegate(
    holder: ViewHolder
): ViewTypeDelegate<Any, ViewHolder> = getViewTypeDelegate(holder.itemViewType)

/**
 * 获取[viewType]对应的[ViewTypeDelegate]
 */
@Suppress("UNCHECKED_CAST")
@Throws(IllegalArgumentException::class)
fun <T : Any> MultiType<T>.getViewTypeDelegate(
    viewType: Int
): ViewTypeDelegate<Any, ViewHolder> = requireNotNull(
    value = keyAt(viewType)?.delegate as? ViewTypeDelegate<Any, ViewHolder>,
    lazyMessage = { "获取ViewTypeDelegate失败，请确认是否未注册ViewType = ${viewType}的Type。" }
)