package com.xiaocydx.sample.common

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.binding.BindingAdapterScope
import com.xiaocydx.cxrv.binding.binding
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.databinding.MenuContainerBinding

interface Menu {
    val text: String
}

fun <T : Menu> MenuContainerBinding.initMenuList(
    block: BindingAdapterScope<T, ItemMenuBinding>.() -> Unit
) = apply { rvMenu.initMenuList(block) }

fun <T : Menu> RecyclerView.initMenuList(
    block: BindingAdapterScope<T, ItemMenuBinding>.() -> Unit
) {
    linear()
    divider(height = 0.5f.dp) {
        color(0xFFD5D5D5.toInt())
    }
    layoutParams(200.dp, matchParent)
    binding(
        uniqueId = { item: T -> item.text },
        inflate = ItemMenuBinding::inflate
    ) {
        onBindView { root.text = it.text }
        block()
    }
}