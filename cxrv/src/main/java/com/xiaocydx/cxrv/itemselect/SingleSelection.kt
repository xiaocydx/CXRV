package com.xiaocydx.cxrv.itemselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.getTag
import androidx.lifecycle.setTagIfAbsent
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

/**
 * 列表单项选择功能的实现类，负责维护状态和更新列表
 *
 * @author xcc
 * @date 2022/4/11
 */
class SingleSelection<ITEM : Any, K : Any> internal constructor(
    adapter: Adapter<*>,
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: Adapter<*>.(position: Int) -> ITEM
) : Selection<ITEM, K>(adapter, itemKey, itemAccess) {
    private val observer = InvalidSelectedObserver()
    private var store: Store<K>? = null
    private var selectedKey: K? = initKey
        set(value) {
            field = value
            store?.selectedKey = value
        }

    init {
        adapter.registerAdapterDataObserver(observer)
    }

    /**
     * 已选择的itemKey，若返回`null`，则表示未选择过
     */
    fun selectedKey(): K? = selectedKey

    /**
     * 已选择的item，若返回`null`，则表示未选择过
     */
    fun selectedItem(): ITEM? = selectedKey?.let(::findItemByKey)

    override fun isSelected(item: ITEM): Boolean {
        val itemKey = item.key ?: return false
        return selectedKey == itemKey
    }

    override fun select(item: ITEM, position: Int): Boolean {
        val itemKey = item.key ?: return false
        if (selectedKey == itemKey) {
            return false
        }
        clearSelected()
        selectedKey = itemKey
        onSelect?.invoke(item)
        notifySelectChanged(position)
        return true
    }

    override fun unselect(item: ITEM, position: Int): Boolean {
        val itemKey = item.key ?: return false
        if (selectedKey != itemKey) {
            return false
        }
        selectedKey = null
        onUnselect?.invoke(item)
        notifySelectChanged(position)
        return true
    }

    /**
     * 清除已选
     *
     * @return `true`表示清除成功，`false`表示未选择过
     */
    fun clearSelected(): Boolean {
        val itemKey = selectedKey ?: return false
        selectedKey = null
        val position = findPositionByKey(itemKey)
        if (position != -1) {
            onUnselect?.invoke(adapter.itemAccess(position))
            notifySelectChanged(position)
        }
        return true
    }

    /**
     * 获取[viewModel]的选择状态作为初始状态，并将后续的选择状态保存至[viewModel]
     */
    override fun initSelected(viewModel: ViewModel): SingleSelection<ITEM, K> {
        var store = viewModel.getTag<Store<K>>(STORE_KEY)
        if (store == null) {
            store = Store(selectedKey)
            viewModel.setTagIfAbsent(STORE_KEY, store)
        } else {
            selectedKey = store.selectedKey
        }
        this.store = store
        return this
    }

    /**
     * 清除[viewModel]的选择状态
     */
    override fun clearSelected(viewModel: ViewModel): SingleSelection<ITEM, K> {
        viewModel.setTagIfAbsent<Store<K>?>(STORE_KEY, null)
        return this
    }

    /**
     * 调用[select]返回`true`时，执行[block]
     */
    override fun onSelect(block: (item: ITEM) -> Unit): SingleSelection<ITEM, K> {
        super.onSelect(block)
        return this
    }

    /**
     * 调用[unselect]返回`true`时，执行[block]
     */
    override fun onUnselect(block: (item: ITEM) -> Unit): SingleSelection<ITEM, K> {
        super.onUnselect(block)
        return this
    }

    fun removeInvalidSelectedObserver() {
        adapter.unregisterAdapterDataObserver(observer)
    }

    private data class Store<K : Any>(var selectedKey: K?)

    private inner class InvalidSelectedObserver : AdapterDataObserver() {

        override fun onChanged() {
            // 数据整体改变，可能有item被移除
            clearInvalidSelected()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            clearInvalidSelected()
        }

        private fun clearInvalidSelected() {
            val itemKey = selectedKey ?: return
            if (findItemByKey(itemKey) == null) {
                selectedKey = null
            }
        }
    }

    private companion object {
        const val STORE_KEY = "com.xiaocydx.cxrv.selection.SingleSelection.STORE_KEY"
    }
}