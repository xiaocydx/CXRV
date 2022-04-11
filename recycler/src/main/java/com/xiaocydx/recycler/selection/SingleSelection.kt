package com.xiaocydx.recycler.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.getTag
import androidx.lifecycle.setTagIfAbsent
import androidx.recyclerview.widget.RecyclerView

/**
 * 列表单项选择功能的实现类，负责维护状态和更新列表
 *
 * @author xcc
 * @date 2022/4/11
 */
class SingleSelection<ITEM : Any, K : Any> internal constructor(
    adapter: RecyclerView.Adapter<*>,
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: RecyclerView.Adapter<*>.(position: Int) -> ITEM
) : Selection<ITEM, K>(adapter, itemKey, itemAccess) {
    private var store: Store<K>? = null
    private var selectedKey: K? = initKey
        set(value) {
            field = value
            store?.selectedKey = value
        }

    /**
     * 已选择的item，返回`null`表示未选择过
     */
    val selectedItem: ITEM?
        get() = selectedKey?.let(::findItemByKey)

    override fun isSelected(item: ITEM): Boolean {
        val itemKey = item.key ?: return false
        return selectedKey == itemKey
    }

    override fun select(item: ITEM, position: Int): Boolean {
        val itemKey = item.key ?: return false
        if (selectedKey == itemKey) {
            return false
        }
        unselectPrevious()
        selectedKey = itemKey
        onSelect?.invoke(item)
        notifySelectChanged(position)
        return true
    }

    /**
     * 取消选择
     *
     * @return 返回`true`表示取消成功，`false`表示未选择过
     */
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

    override fun saveToViewModel(viewModel: ViewModel) {
        var store = viewModel.getTag<Store<K>>(STORE_KEY)
        if (store == null) {
            store = Store(selectedKey)
            viewModel.setTagIfAbsent(STORE_KEY, store)
        } else {
            selectedKey = store.selectedKey
        }
        this.store = store
    }

    override fun clearFromViewModel(viewModel: ViewModel) {
        viewModel.setTagIfAbsent<Store<K>?>(STORE_KEY, null)
    }

    private fun unselectPrevious() {
        val itemKey = selectedKey ?: return
        selectedKey = null
        val position = findPositionByKey(itemKey)
        if (position != -1) {
            onUnselect?.invoke(adapter.itemAccess(position))
            notifySelectChanged(position)
        }
    }

    override fun onChanged() {
        // 数据整体改变，可能有item被移除
        clearInvalidSelected()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        clearInvalidSelected()
    }

    /**
     * 当[onChanged]或[onItemRangeRemoved]触发时，
     * 清除[findItemByKey]无法查到item的选择结果。
     */
    private fun clearInvalidSelected() {
        val itemKey = selectedKey ?: return
        if (findItemByKey(itemKey) == null) {
            selectedKey = null
        }
    }

    private data class Store<K : Any>(var selectedKey: K?)

    private companion object {
        const val STORE_KEY = "com.xiaocydx.recycler.selection.SingleSelection.STORE_KEY"
    }
}