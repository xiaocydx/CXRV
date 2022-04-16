package com.xiaocydx.recycler.extension

import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.selection.MultiSelection
import com.xiaocydx.recycler.selection.SingleSelection

/**
 * 列表单项选择功能，负责维护状态和更新列表
 *
 * 1. 当选择新的item时，会取消上一个item的选择。
 * 2. 调用[toggleSelect]可以对同一个item切换选择/取消选择。
 * 3. 当[onChanged]、[onItemRangeRemoved]触发时，会清除无效的`itemKey`。
 *
 * 单项选择的基本配置流程：
 * ```
 * class FooAdapter : RecyclerView.Adapter<ViewHolder> {
 *     // 第一步：初始化SingleSelection
 *     private val selection = singleSelection(
 *         itemKey = { item -> item.id },
 *         itemAccess = { position -> getItem(position) }
 *     ).onSelect {
 *         ...
 *     }.onUnselect {
 *         ...
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
@Suppress("UNCHECKED_CAST", "KDocUnresolvedReference")
fun <AdapterT : Adapter<*>, ITEM : Any, K : Any> AdapterT.singleSelection(
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: AdapterT.(position: Int) -> ITEM
): SingleSelection<ITEM, K> {
    return SingleSelection(this, initKey, itemKey, itemAccess as Adapter<*>.(Int) -> ITEM)
}

/**
 * 列表多项选择功能的实现类，负责维护状态和更新列表
 *
 * 1. 调用[toggleSelect]可以对同一个item切换选择/取消选择。
 * 2. 当[onChanged]、[onItemRangeRemoved]触发时，会清除无效的`itemKey`。
 * 3. 当[onChanged]、[onItemRangeRemoved]、[onItemRangeInserted]触发时，会检查全选状态。
 *
 * 多项选择的基本配置流程：
 * ```
 * class FooAdapter : RecyclerView.Adapter<ViewHolder> {
 *     // 第一步：初始化MultiSelection
 *     private val selection = multiSelection(
 *         itemKey = { item -> item.id },
 *         itemAccess = { position -> getItem(position) }
 *     ).onSelect {
 *         ...
 *     }.onUnselect {
 *         ...
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
@Suppress("UNCHECKED_CAST", "KDocUnresolvedReference")
fun <AdapterT : Adapter<*>, ITEM : Any, K : Any> AdapterT.multiSelection(
    maxSelectSize: Int = Int.MAX_VALUE,
    initKeys: List<K>? = null,
    itemKey: (item: ITEM) -> K?,
    itemAccess: AdapterT.(position: Int) -> ITEM
): MultiSelection<ITEM, K> {
    require(maxSelectSize > 0) {
        "maxSelectSize = $maxSelectSize，值需要大于0"
    }
    require(initKeys == null || initKeys.size <= maxSelectSize) {
        "initKeys.size = ${initKeys!!.size}，size需要小于或等于maxSelectSize"
    }
    return MultiSelection(this, initKeys, itemKey, itemAccess as Adapter<*>.(Int) -> ITEM, maxSelectSize)
}

/**
 * 列表单项选择功能
 *
 * 该函数结合[ListAdapter]的能力，简化[singleSelection]的初始化代码：
 * ```
 * val selection = singleSelection(itemKey = { item -> item.id })
 * ```
 * 单项选择功能的配置流程以及函数描述，请看[singleSelection]的注释。
 */
fun <ITEM : Any, K : Any> ListAdapter<ITEM, *>.singleSelection(
    initKey: K? = null,
    itemKey: (item: ITEM) -> K?
): SingleSelection<ITEM, K> = singleSelection(
    initKey = initKey,
    itemKey = itemKey,
    itemAccess = { getItem(it) }
)

/**
 * 列表多项选择功能
 *
 * 该函数结合[ListAdapter]的能力，简化[multiSelection]的初始化代码：
 * ```
 * val selection = multiSelection(itemKey = { item -> item.id })
 * ```
 * 多项选择功能的配置流程以及函数描述，请看[multiSelection]的注释。
 */
fun <ITEM : Any, K : Any> ListAdapter<ITEM, *>.multiSelection(
    maxSelectSize: Int = Int.MAX_VALUE,
    initKeys: List<K>? = null,
    itemKey: (item: ITEM) -> K?
): MultiSelection<ITEM, K> = multiSelection(
    maxSelectSize = maxSelectSize,
    initKeys = initKeys,
    itemKey = itemKey,
    itemAccess = { getItem(it) }
)