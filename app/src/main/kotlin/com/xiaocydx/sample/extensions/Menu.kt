package com.xiaocydx.sample.extensions

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingAdapterScope
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.databinding.MenuContainerBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

interface Menu {
    val text: String
}

fun <T : Menu> MenuContainerBinding.initMenuList(
    block: BindingAdapterScope<T, ItemMenuBinding>.() -> Unit
) = apply { rvMenu.initMenuList(block) }

fun <T : Menu> RecyclerView.initMenuList(
    block: BindingAdapterScope<T, ItemMenuBinding>.() -> Unit
) {
    linear().fixedSize()
    divider(height = 0.5f.dp) {
        color(0xFFD5D5D5.toInt())
    }
    layoutParams(200.dp, matchParent)
    adapter(bindingAdapter(
        uniqueId = { item: T -> item.text },
        inflate = ItemMenuBinding::inflate
    ) {
        onBindView { root.text = it.text }
        block()
    })
}