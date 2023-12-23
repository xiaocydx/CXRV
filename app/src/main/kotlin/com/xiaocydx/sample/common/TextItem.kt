@file:Suppress("FunctionName")

package com.xiaocydx.sample.common

import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import kotlin.math.max

data class TextItem(val text: String, val type: String)

fun TextType1Delegate() = bindingDelegate(
    uniqueId = TextItem::text,
    inflate = ItemTextType1Binding::inflate
) {
    typeLinker { it.type == "type1" }
    onBindView { textView.text = it.text }
}

fun TextType2Delegate() = bindingDelegate(
    uniqueId = TextItem::text,
    inflate = ItemTextType2Binding::inflate
) {
    typeLinker { it.type == "type2" }
    onBindView { textView.text = it.text }
}

fun ListAdapter<TextItem, *>.initTextItems(
    itemSize: Int = 20,
    textPrefix: String = "",
    type: String = "type1"
): ListAdapter<TextItem, *> {
    val end = max(1, itemSize)
    submitList((1..end).map {
        TextItem(text = "$textPrefix Text-$it", type)
    })
    return this
}

fun ListAdapter<TextItem, *>.initMultiTypeTextItems(
    itemSize: Int = 20,
    textPrefix: String = ""
): ListAdapter<TextItem, *> {
    val end = max(1, itemSize)
    submitList((1..end).map {
        TextItem(
            text = "$textPrefix Text-$it",
            type = if (it % 2 == 0) "type2" else "type1"
        )
    })
    return this
}