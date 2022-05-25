package com.xiaocydx.cxrv.itemselect

import androidx.collection.ArraySet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.getTag
import androidx.lifecycle.setTagIfAbsent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.cacheViews
import kotlin.math.max
import kotlin.math.min

/**
 * 列表多项选择功能的实现类，负责维护状态和更新列表
 *
 * @author xcc
 * @date 2022/4/11
 */
class MultiSelection<ITEM : Any, K : Any> internal constructor(
    adapter: Adapter<*>,
    initKeys: List<K>? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: Adapter<*>.(position: Int) -> ITEM,
    private val maxSelectSize: Int = Int.MAX_VALUE
) : Selection<ITEM, K>(adapter, itemKey, itemAccess) {
    private val observer = InvalidSelectedObserver()
    private var _selectedKeys: ArraySet<K>? = initKeys?.let(::ArraySet)
    private var isSelectedAllState: Boolean = false
    private var onSelectedMax: (() -> Unit)? = null
    private var onSelectAllStateChange: ((Boolean) -> Unit)? = null
    private val selectedKeys: ArraySet<K>
        get() {
            if (_selectedKeys == null) {
                _selectedKeys = ArraySet()
            }
            return _selectedKeys!!
        }

    init {
        adapter.registerAdapterDataObserver(observer)
    }

    /**
     * 是否已到选择上限
     */
    val isSelectedMax: Boolean
        get() = selectedSize == maxSelectSize

    /**
     * 是否已选择全部
     */
    val isSelectedAll: Boolean
        get() = selectedSize == adapter.itemCount

    /**
     * 已选择的item数量
     */
    val selectedSize: Int
        get() = selectedKeys.size

    /**
     * 已选择的itemKey集合
     */
    fun selectedKeys(): List<K> = selectedKeys.toList()

    /**
     * 已选择的item集合
     */
    fun selectedItems(): List<ITEM> = when {
        selectedKeys.isEmpty() -> emptyList()
        else -> selectedKeys.mapNotNull { findItemByKey(it) }
    }

    override fun isSelected(item: ITEM): Boolean {
        return item.key?.let(selectedKeys::contains) ?: false
    }

    override fun select(item: ITEM, position: Int): Boolean {
        val itemKey = item.key ?: return false
        if (isSelectedMax) {
            onSelectedMax?.invoke()
            return false
        }
        if (isSelectedAll) {
            return false
        }
        if (selectedKeys.add(itemKey)) {
            onSelect?.invoke(item)
            if (isSelectedAll) {
                // 未全选状态改为全选状态
                updateSelectedAllState()
            }
            notifySelectChanged(position)
            return true
        }
        return false
    }

    override fun unselect(item: ITEM, position: Int): Boolean {
        val itemKey = item.key ?: return false
        val wasSelectedAll = isSelectedAll
        if (selectedKeys.remove(itemKey)) {
            onUnselect?.invoke(item)
            if (wasSelectedAll) {
                // 全选状态改为未全选状态
                updateSelectedAllState()
            }
            notifySelectChanged(position)
            return true
        }
        return false
    }

    /**
     * 选择全部item
     *
     * **注意**：若设置了[maxSelectSize]，则调用该函数会抛出[IllegalArgumentException]异常。
     *
     * @return `true`表示选择成功，`false`表示已全选
     */
    fun selectAll(parent: RecyclerView): Boolean {
        require(maxSelectSize == Int.MAX_VALUE) {
            "已设置maxSelectSize，不能调用selectAll()。"
        }
        if (isSelectedAll) {
            return false
        }
        // 在修改selectedKeys之前，找到更新范围
        val positions = parent.findChangePositions(isSelect = true)
        for (position in 0 until adapter.itemCount) {
            val item = adapter.itemAccess(position)
            val itemKey = itemKey(item) ?: continue
            selectedKeys.add(itemKey)
        }
        updateSelectedAllState()
        positions?.also {
            notifySelectRangeChanged(it.first(), it.last())
        }
        return true
    }

    /**
     * 清除全部已选
     *
     * @return `true`表示清除成功，`false`表示未选择过
     */
    fun clearSelected(parent: RecyclerView): Boolean {
        if (selectedKeys.isEmpty()) {
            return false
        }
        // 在修改selectedKeys之前，找到更新范围
        val positions = parent.findChangePositions(isSelect = false)
        selectedKeys.clear()
        updateSelectedAllState()
        positions?.also {
            notifySelectRangeChanged(it.first(), it.last())
        }
        return true
    }

    /**
     * 获取[viewModel]的选择状态作为初始状态，并将后续的选择状态保存至[viewModel]
     */
    override fun initSelected(viewModel: ViewModel): MultiSelection<ITEM, K> {
        var keys = viewModel.getTag<ArraySet<K>>(STORE_KEY)
        if (keys == null) {
            keys = selectedKeys
            viewModel.setTagIfAbsent(STORE_KEY, keys)
        } else {
            _selectedKeys = keys
        }
        return this
    }

    /**
     * 清除[viewModel]的选择状态
     */
    override fun clearSelected(viewModel: ViewModel): MultiSelection<ITEM, K> {
        viewModel.setTagIfAbsent<ArraySet<K>?>(STORE_KEY, null)
        return this
    }

    /**
     * 调用[select]返回`true`时，执行[block]
     */
    override fun onSelect(block: (item: ITEM) -> Unit): MultiSelection<ITEM, K> {
        super.onSelect(block)
        return this
    }

    /**
     * 调用[unselect]返回`true`时，执行[block]
     */
    override fun onUnselect(block: (item: ITEM) -> Unit): MultiSelection<ITEM, K> {
        super.onUnselect(block)
        return this
    }

    /**
     * 调用[select]时，若[isSelectedMax]为`true`，则执行[block]
     */
    fun onSelectedMax(block: () -> Unit): MultiSelection<ITEM, K> {
        onSelectedMax = block
        return this
    }

    /**
     * 全选状态改变时，执行[block]
     *
     * 全选状态包含以下三种改变情况：
     * * 若列表由未全选状态改为全选状态，则执行[block]，此时[isSelectedAll]为true。
     * * 若列表由全选状态改为未全选状态，则执行[block]，此时[isSelectedAll]为false。
     * * 若列表处于全选状态，则插入item时，会改为未全选状态，执行[block]，此时[isSelectedAll]为false。
     */
    fun onSelectAllStateChange(block: (isSelectedAll: Boolean) -> Unit): MultiSelection<ITEM, K> {
        onSelectAllStateChange = block
        return this
    }

    fun removeInvalidSelectedObserver() {
        adapter.unregisterAdapterDataObserver(observer)
    }

    private fun updateSelectedAllState() {
        val isSelectedAll = isSelectedAll
        if (isSelectedAllState != isSelectedAll) {
            isSelectedAllState = isSelectedAll
            onSelectAllStateChange?.invoke(isSelectedAll)
        }
    }

    /**
     * 尝试缩小更新范围，减少不必要的更新
     */
    private fun RecyclerView.findChangePositions(isSelect: Boolean): IntArray? {
        var startPosition = -1
        var endPosition = -1
        val adapter = this@MultiSelection.adapter

        // 确定子View更新范围
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val holder = getChildViewHolder(child)
            if (holder.bindingAdapter != adapter) {
                continue
            }
            val position = holder.bindingAdapterPosition
            val itemKey = adapter.itemAccess(position).key ?: continue
            val isContains = selectedKeys.contains(itemKey)
            if ((isSelect && !isContains) || (!isSelect && isContains)) {
                if (startPosition == -1) {
                    startPosition = position
                }
                endPosition = position
            }
        }

        // 离屏缓存可能扩大更新范围
        val cacheViews = cacheViews
        for (index in cacheViews.indices) {
            val holder = cacheViews[index]
            if (holder.bindingAdapter != adapter) {
                continue
            }
            val position = cacheViews[index].bindingAdapterPosition
            val itemKey = adapter.itemAccess(position).key ?: continue
            val isContains = selectedKeys.contains(itemKey)
            if ((isSelect && !isContains) || (!isSelect && isContains)) {
                startPosition = min(position, startPosition)
                endPosition = max(position, endPosition)
            }
        }

        if (startPosition in 0..endPosition) {
            val positions = IntArray(2)
            positions[0] = startPosition
            positions[1] = endPosition
            return positions
        }
        return null
    }

    private inner class InvalidSelectedObserver : AdapterDataObserver() {

        override fun onChanged() {
            // 数据整体改变，可能有item被移除
            clearInvalidSelected()
            updateSelectedAllState()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            clearInvalidSelected()
            updateSelectedAllState()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateSelectedAllState()
        }

        private fun clearInvalidSelected() {
            for (index in (selectedSize - 1) downTo 0) {
                val itemKey = selectedKeys.valueAt(index) ?: continue
                if (findItemByKey(itemKey) == null) {
                    selectedKeys.removeAt(index)
                }
            }
        }
    }

    private companion object {
        const val STORE_KEY = "com.xiaocydx.cxrv.selection.MultiSelection.STORE_KEY"
    }
}