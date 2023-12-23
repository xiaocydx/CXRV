package com.xiaocydx.sample.common

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.wrapContent
import com.xiaocydx.cxrv.binding.BindingAdapterScope
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.sample.databinding.ActionContainerBinding
import com.xiaocydx.sample.databinding.ActionContentBinding
import com.xiaocydx.sample.databinding.ItemButtonBinding

interface Action {
    val text: String
}

fun <T : Action> ActionContainerBinding.initActionList(
    block: BindingAdapterScope<T, ItemButtonBinding>.() -> Unit
) = apply { rvAction.initActionList(block) }

fun <T : Action> ActionContentBinding.initActionList(
    block: BindingAdapterScope<T, ItemButtonBinding>.() -> Unit
) = apply { rvAction.initActionList(block) }

fun <T : Action> RecyclerView.initActionList(
    block: BindingAdapterScope<T, ItemButtonBinding>.() -> Unit
) {
    linear(RecyclerView.HORIZONTAL)
    divider(10.dp, 10.dp) { edge(Edge.all()) }
    layoutParams(matchParent, wrapContent)
    adapter(bindingAdapter(
        uniqueId = { item: T -> item.text },
        inflate = ItemButtonBinding::inflate
    ) {
        onBindView { root.text = it.text }
        block()
    })
}