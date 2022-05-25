package com.xiaocydx.cxrv.list

/**
 * 只可读，不可修改的List
 *
 * @author xcc
 * @date 2021/9/11
 */
internal data class ReadOnlyList<E>(
    private val list: List<E>,
) : List<E> by list {

    //region list的迭代器可能是Mutable实现
    override fun iterator(): Iterator<E> {
        return ReadOnlyIterator(list.iterator())
    }

    override fun listIterator(): ListIterator<E> {
        return ReadOnlyListIterator(list.listIterator())
    }

    override fun listIterator(index: Int): ListIterator<E> {
        return ReadOnlyListIterator(list.listIterator(index))
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        return ReadOnlyList(list.subList(fromIndex, toIndex))
    }
    //endregion

    private class ReadOnlyIterator<E>(
        private val iterator: Iterator<E>
    ) : Iterator<E> by iterator

    private class ReadOnlyListIterator<E>(
        private val listIterator: ListIterator<E>
    ) : ListIterator<E> by listIterator
}

internal fun <E> List<E>.toReadOnlyList(): List<E> {
    return if (this !is ReadOnlyList<E>) ReadOnlyList(this) else this
}