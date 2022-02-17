package com.xiaocydx.recycler.list

import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * ViewHolder绑定监听
 *
 * @author xcc
 * @date 2021/11/26
 */
internal fun interface ViewHolderListener<VH : ViewHolder> {

    fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>)
}