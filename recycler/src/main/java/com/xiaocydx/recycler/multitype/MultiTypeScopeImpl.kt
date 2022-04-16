package com.xiaocydx.recycler.multitype

import com.xiaocydx.recycler.list.ListAdapter

/**
 * 多类型作用域实现类
 *
 * @author xcc
 * @date 2022/4/16
 */
@PublishedApi
internal class MultiTypeScopeImpl<T : Any> : MultiTypeScope<T>() {
    private var _multiType: MutableMultiType<T>? = MutableMultiTypeImpl()
    private var _listAdapter: ListAdapter<T, *>? = MultiTypeAdapter()
    private val multiType: MutableMultiType<T>
        get() = checkNotNull(_multiType) { "已完成多类型注册" }
    override val listAdapter: ListAdapter<T, *>
        get() = checkNotNull(_listAdapter) { "已完成多类型注册" }

    override val size: Int
        get() = multiType.size

    override fun keyAt(viewType: Int): Type<out T>? {
        return multiType.keyAt(viewType)
    }

    override fun valueAt(index: Int): Type<out T> {
        return multiType.valueAt(index)
    }

    override fun itemAt(item: T): Type<out T>? {
        return multiType.itemAt(item)
    }

    override fun register(type: Type<out T>) {
        multiType.register(type)
    }

    fun complete(): ListAdapter<T, *> {
        (multiType as? MutableMultiTypeImpl)?.complete()
        (listAdapter as? MultiTypeAdapter)?.setMultiType(multiType)
        val adapter = listAdapter
        _listAdapter = null
        _multiType = null
        return adapter
    }
}