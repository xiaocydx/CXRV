package com.xiaocydx.recycler.extension

import androidx.collection.ArraySet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.getTag
import androidx.lifecycle.setTagIfAbsent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.cacheViews
import androidx.recyclerview.widget.payloads
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import kotlin.math.max
import kotlin.math.min

/**
 * 列表单项选择功能
 *
 * 该函数结合[ListAdapter]的能力，简化[SingleSelection]的初始化代码：
 * ```
 * val selection = singleSelection(itemKey = { item -> item.id })
 * ```
 * 单项选择功能的配置流程以及函数描述，请看[SingleSelection]的注释。
 */
fun <ITEM : Any, K : Any> ListAdapter<ITEM, *>.singleSelection(
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?
): SingleSelection<*, ITEM, K> = SingleSelection(
    adapter = this,
    initKey = initKey,
    itemKey = itemKey,
    itemAccess = { getItem(it) }
)

/**
 * 列表多项选择功能
 *
 * 该函数结合[ListAdapter]的能力，简化[MultiSelection]的初始化代码：
 * ```
 * val selection = multiSelection(itemKey = { item -> item.id })
 * ```
 * 多项选择功能的配置流程以及函数描述，请看[MultiSelection]的注释。
 */
fun <ITEM : Any, K : Any> ListAdapter<ITEM, *>.multiSelection(
    maxSelectSize: Int = Int.MAX_VALUE,
    itemKey: (item: ITEM) -> K?
): MultiSelection<*, ITEM, K> = MultiSelection(
    adapter = this,
    maxSelectSize = maxSelectSize,
    itemKey = itemKey,
    itemAccess = { getItem(it) }
)

/**
 * 列表选择功能，实现类有[SingleSelection]和[MultiSelection]
 */
sealed class Selection<AdapterT : Adapter<*>, ITEM : Any, K : Any>(
    protected val adapter: AdapterT,
    protected val itemKey: (item: ITEM) -> K?,
    protected val itemAccess: AdapterT.(position: Int) -> ITEM
) : AdapterDataObserver() {
    protected var onSelect: ((ITEM) -> Unit)? = null
    protected var onUnselect: ((ITEM) -> Unit)? = null

    /**
     * 通过[itemKey]获取`key`
     */
    protected val ITEM.key: K?
        get() = itemKey(this)

    /**
     * 通过[itemAccess]获取`item`
     */
    protected val ViewHolder.item: ITEM?
        get() {
            if (bindingAdapter != adapter) {
                return null
            }
            return adapter.itemAccess(bindingAdapterPosition)
        }

    /**
     * 通过[ViewHolder]获取`itemKey`
     */
    protected val ViewHolder.itemKey: K?
        get() = item?.key

    init {
        @Suppress("LeakingThis")
        adapter.registerAdapterDataObserver(this)
    }

    /**
     * [holder]中是否包含[Payload]，用于在[Adapter.onBindViewHolder]中判断payload刷新。
     */
    fun hasPayload(holder: ViewHolder): Boolean {
        return holder.payloads.contains(Payload)
    }

    /**
     * 切换选择/取消选择
     *
     * @return 返回`true`表示切换成功，`false`表示切换失败
     */
    fun toggleSelect(holder: ViewHolder): Boolean {
        return if (!isSelected(holder)) {
            select(holder)
        } else {
            unselect(holder)
        }
    }

    /**
     * 选择
     *
     * @return 返回`true`表示选择成功，`false`表示选择失败
     */
    abstract fun select(holder: ViewHolder): Boolean

    /**
     * 取消选择
     *
     * @return 返回`true`表示取消成功，`false`表示取消失败
     */
    abstract fun unselect(holder: ViewHolder): Boolean

    /**
     * 是否已选择
     */
    abstract fun isSelected(holder: ViewHolder): Boolean

    /**
     * 将选择状态保存至[viewModel]
     */
    abstract fun saveToViewModel(viewModel: ViewModel)

    /**
     * 将选择状态从[viewModel]中清除
     */
    abstract fun clearFromViewModel(viewModel: ViewModel)

    /**
     * 调用[select]返回`true`时，执行[block]
     */
    fun onSelect(block: (item: ITEM) -> Unit) {
        onSelect = block
    }

    /**
     * 调用[unselect]返回`true`时，执行[block]
     */
    fun onUnselect(block: (item: ITEM) -> Unit) {
        onUnselect = block
    }

    protected fun notifySelectChanged(position: Int) {
        adapter.notifyItemChanged(position, Payload)
    }

    protected fun notifySelectRangeChanged(startPosition: Int, endPosition: Int) {
        val itemCount = endPosition - startPosition + 1
        adapter.notifyItemRangeChanged(startPosition, itemCount, Payload)
    }

    protected fun findSelectedPosition(itemKey: K): Int {
        for (position in 0 until adapter.itemCount) {
            val item = adapter.itemAccess(position)
            if (itemKey(item) == itemKey) {
                return position
            }
        }
        return -1
    }

    protected fun findSelectedItem(itemKey: K): ITEM? {
        for (position in 0 until adapter.itemCount) {
            val item = adapter.itemAccess(position)
            if (itemKey(item) == itemKey) {
                return item
            }
        }
        return null
    }

    companion object {
        const val Payload = "com.xiaocydx.recycler.extension.Selection"
    }
}

/**
 * 列表单项选择功能的实现类，负责维护状态和更新列表
 *
 * * 当选择新的item时，会取消上一个item的选择。
 * * 调用[SingleSelection.toggleSelect]可以对同一个item切换选择/取消选择。
 * * 当[onChanged]、[onItemRangeRemoved]触发时，会清除无效的`itemKey`。
 *
 * 单项选择的基本配置流程：
 * ```
 * class FooAdapter : ListAdapter<Foo, ViewHolder> {
 *     // 第一步：初始化SingleSelection
 *     private val selection = SingleSelection(
 *         adapter = this,
 *         itemKey = { item -> item.id },
 *         itemAccess = { position -> getItem(position) }
 *     ).apply {
 *         // 选择时执行代码块
 *         onSelect { ... }
 *         // 取消选择时执行代码块
 *         onUnselect { ... }
 *     }
 *
 *     // 第二步：点击itemView时调用单项选择
 *     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
 *          val itemView = LayoutInflater.from(parent.context)
 *                 .inflate(R.layout.item_foo, parent, false)
 *          val holder = ViewHolder(itemView)
 *          itemView.setOnClickListener {
 *              selection.select(holder)
 *          }
 *          return holder
 *     }
 *
 *     // 第三步：添加选择视图的更新逻辑
 *     override fun onBindViewHolder(holder: ViewHolder, item: Foo, payloads: List<Any>) {
 *         // Payload的值为Selection.Payload
 *         if(selection.hasPayload(holder)) {
 *             // 仅更新选择视图
 *             holder.selectView.isVisible = selection.isSelected(holder)
 *         } else {
 *             // 更新全部视图
 *             holder.selectView.isVisible = selection.isSelected(holder)
 *             ...
 *         }
 *     }
 * }
 * ```
 */
class SingleSelection<AdapterT : Adapter<*>, ITEM : Any, K : Any>(
    adapter: AdapterT,
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: AdapterT.(position: Int) -> ITEM
) : Selection<AdapterT, ITEM, K>(adapter, itemKey, itemAccess) {
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
        get() = selectedKey?.let(::findSelectedItem)

    /**
     * 选择
     *
     * @return 返回`true`表示选择成功，`false`表示选择过
     */
    override fun select(holder: ViewHolder): Boolean {
        val item = holder.item ?: return false
        val itemKey = item.key ?: return false
        if (selectedKey == itemKey) {
            return false
        }
        unselectPrevious()
        selectedKey = itemKey
        onSelect?.invoke(item)
        notifySelectChanged(holder.bindingAdapterPosition)
        return true
    }

    /**
     * 取消选择
     *
     * @return 返回`true`表示取消成功，`false`表示未选择过
     */
    override fun unselect(holder: ViewHolder): Boolean {
        val item = holder.item ?: return false
        val itemKey = item.key ?: return false
        if (selectedKey != itemKey) {
            return false
        }
        selectedKey = null
        onUnselect?.invoke(item)
        notifySelectChanged(holder.bindingAdapterPosition)
        return true
    }

    override fun isSelected(holder: ViewHolder): Boolean {
        val itemKey = holder.itemKey ?: return false
        return selectedKey == itemKey
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
        val position = findSelectedPosition(itemKey)
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
     * 清除[findSelectedItem]无法查到item的选择结果。
     */
    private fun clearInvalidSelected() {
        val itemKey = selectedKey ?: return
        if (findSelectedItem(itemKey) == null) {
            selectedKey = null
        }
    }

    private data class Store<K : Any>(var selectedKey: K?)

    private companion object {
        const val STORE_KEY = "com.xiaocydx.recycler.extension.SingleSelection.STORE_KEY"
    }
}

/**
 * 列表多项选择功能的实现类，负责维护状态和更新列表
 *
 * * 调用[MultiSelection.toggleSelect]可以对同一个item切换选择/取消选择。
 * * 当[onChanged]、[onItemRangeRemoved]触发时，会清除无效的`itemKey`。
 * * 当[onChanged]、[onItemRangeRemoved]、[onItemRangeInserted]触发时，会检查全选状态，
 * 全选的描述请看[selectAll]和[updateSelectedAllState]。
 *
 * 多项选择的基本配置流程：
 * ```
 * class FooAdapter : ListAdapter<Foo, ViewHolder> {
 *     // 第一步：初始化MultiSelection
 *     private val selection = MultiSelection(
 *         adapter = this,
 *         itemKey = { item -> item.id },
 *         itemAccess = { position -> getItem(position) }
 *     ).apply {
 *         // 选择时执行代码块
 *         onSelect { ... }
 *         // 取消选择时执行代码块
 *         onUnselect { ... }
 *     }
 *
 *     // 第二步：点击itemView时调用多项选择
 *     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
 *          val itemView = LayoutInflater.from(parent.context)
 *                 .inflate(R.layout.item_foo, parent, false)
 *          val holder = ViewHolder(itemView)
 *          itemView.setOnClickListener {
 *              selection.select(holder)
 *          }
 *          return holder
 *     }
 *
 *     // 第三步：添加选择视图的更新逻辑
 *     override fun onBindViewHolder(holder: ViewHolder, item: Foo, payloads: List<Any>) {
 *         // Payload的值为Selection.Payload
 *         if(selection.hasPayload(holder)) {
 *             // 仅更新选择视图
 *             holder.selectView.isVisible = selection.isSelected(holder)
 *         } else {
 *             // 更新全部视图
 *             holder.selectView.isVisible = selection.isSelected(holder)
 *             ...
 *         }
 *     }
 * }
 * ```
 */
class MultiSelection<AdapterT : Adapter<*>, ITEM : Any, K : Any>(
    adapter: AdapterT,
    itemKey: (item: ITEM) -> K?,
    itemAccess: AdapterT.(position: Int) -> ITEM,
    val maxSelectSize: Int = Int.MAX_VALUE
) : Selection<AdapterT, ITEM, K>(adapter, itemKey, itemAccess) {
    private var _selectedKeys: ArraySet<K>? = null
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

    /**
     * 是否已到[maxSelectSize]上限
     */
    val isSelectedMax: Boolean
        get() = selectedSize == maxSelectSize

    /**
     * 是否选择了全部item
     */
    val isSelectedAll: Boolean
        get() = selectedSize == adapter.itemCount

    /**
     * 已选择的item数量
     */
    val selectedSize: Int
        get() = selectedKeys.size

    /**
     * 已选择的item集合
     */
    val selectedList: List<ITEM>
        get() {
            if (selectedKeys.isEmpty()) {
                return emptyList()
            }
            val selected = ArrayList<ITEM>(selectedSize)
            for (itemKey in selectedKeys) {
                val item = findSelectedItem(itemKey) ?: continue
                selected.add(item)
            }
            return selected
        }

    init {
        require(maxSelectSize > 0) { "maxSelectSize的值必须大于0" }
    }

    /**
     * 选择
     *
     * @return 返回`true`表示选择成功，`false`表示选择过或者已到[maxSelectSize]上限
     */
    override fun select(holder: ViewHolder): Boolean {
        val item = holder.item ?: return false
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
            notifySelectChanged(holder.bindingAdapterPosition)
            return true
        }
        return false
    }

    /**
     * 取消选择
     *
     * @return 返回`true`表示取消成功，`false`表示未选择过
     */
    override fun unselect(holder: ViewHolder): Boolean {
        val item = holder.item ?: return false
        val itemKey = item.key ?: return false
        val wasSelectedAll = isSelectedAll
        if (selectedKeys.remove(itemKey)) {
            onUnselect?.invoke(item)
            if (wasSelectedAll) {
                // 全选状态改为未全选状态
                updateSelectedAllState()
            }
            notifySelectChanged(holder.bindingAdapterPosition)
            return true
        }
        return false
    }

    /**
     * 选择全部item
     *
     * **注意**：若设置了[maxSelectSize]，则调用[selectAll]会抛出异常。
     *
     * @return 返回`true`表示选择成功，`false`表示已全选
     */
    @Throws(IllegalArgumentException::class)
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
     * @return 返回`true`表示清除成功，`false`表示未选择过
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

    override fun isSelected(holder: ViewHolder): Boolean {
        return holder.itemKey?.let(selectedKeys::contains) ?: false
    }

    override fun saveToViewModel(viewModel: ViewModel) {
        var keys = viewModel.getTag<ArraySet<K>>(STORE_KEY)
        if (keys == null) {
            keys = selectedKeys
            viewModel.setTagIfAbsent(STORE_KEY, keys)
        } else {
            _selectedKeys = keys
        }
    }

    override fun clearFromViewModel(viewModel: ViewModel) {
        viewModel.setTagIfAbsent<ArraySet<K>?>(STORE_KEY, null)
    }

    /**
     * 调用[select]时，若[isSelectedMax]为`true`，则执行[block]
     */
    fun onSelectedMax(block: () -> Unit) {
        onSelectedMax = block
    }

    /**
     * 全选状态改变时，执行[block]
     *
     * 全选状态包含以下三种改变情况：
     * * 若列表由未全选状态改为全选状态，则执行[block]，此时[isSelectedAll]为true。
     * * 若列表由全选状态改为未全选状态，则执行[block]，此时[isSelectedAll]为false。
     * * 若列表处于全选状态，则插入item时，会改为未全选状态，执行[block]，此时[isSelectedAll]为false。
     */
    fun onSelectAllStateChange(block: (isSelectedAll: Boolean) -> Unit) {
        onSelectAllStateChange = block
    }

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

    /**
     * 当[onChanged]或[onItemRangeRemoved]触发时，
     * 清除[findSelectedItem]无法查到item的选择结果。
     */
    private fun clearInvalidSelected() {
        for (index in (selectedSize - 1) downTo 0) {
            val itemKey = selectedKeys.valueAt(index) ?: continue
            if (findSelectedItem(itemKey) == null) {
                selectedKeys.removeAt(index)
            }
        }
    }

    /**
     * 当[onChanged]、[onItemRangeRemoved]、[onItemRangeInserted]触发时，
     * 在[clearInvalidSelected]执行之后，更新全选状态。
     */
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

    private companion object {
        const val STORE_KEY = "com.xiaocydx.recycler.extension.MultiSelection.STORE_KEY"
    }
}