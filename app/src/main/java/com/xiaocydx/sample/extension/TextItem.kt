package com.xiaocydx.sample.extension

import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ItemTextTypeBinding
import kotlin.math.max

data class TextItem(val text: String, val type: String)

fun getTextType1Delegate(): BindingDelegate<TextItem, ItemTextTypeBinding> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextTypeBinding::inflate
    ) {
        typeLinker { it.type == "type1" }
        onCreateView {
            textView.setBackgroundResource(R.drawable.selector_text_type1)
            targetView.setBackgroundResource(R.drawable.selector_text_type1_target)
        }
        onBindView { textView.text = it.text }
    }
}

fun getTextType2Delegate(): BindingDelegate<TextItem, ItemTextTypeBinding> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextTypeBinding::inflate
    ) {
        typeLinker { it.type == "type2" }
        onCreateView {
            textView.setBackgroundResource(R.drawable.selector_text_type2)
            targetView.setBackgroundResource(R.drawable.selector_text_type2_target)
        }
        onBindView { textView.text = it.text }
    }
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