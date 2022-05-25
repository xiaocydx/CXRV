package com.xiaocydx.cxrv.internal

import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * 是否有可显示的item
 */
internal val Adapter<*>.hasDisplayItem: Boolean
    get() = itemCount > 0

/**
 * [position]是否为第一个可显示的item
 */
internal fun Adapter<*>.isFirstDisplayItem(position: Int): Boolean {
    return hasDisplayItem && position == 0
}

/**
 * [position]是否为最后一个可显示的item
 */
internal fun Adapter<*>.isLastDisplayItem(position: Int): Boolean {
    return position == itemCount - 1
}