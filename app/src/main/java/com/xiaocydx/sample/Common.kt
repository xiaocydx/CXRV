package com.xiaocydx.sample

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xiaocydx.recycler.binding.bindingDelegate
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.submitList
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import kotlin.math.max

val Float.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    ).let { if (it >= 0) it + 0.5f else it - 0.5f }.toInt()

val Double.dp: Int
    get() = toFloat().dp

val Int.dp: Int
    get() = toFloat().dp

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Fragment.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(text, duration)
}

data class TextItem(val text: String, val type: String)

fun getTextType1Delegate(): ViewTypeDelegate<TextItem, *> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextType1Binding::inflate
    ) {
        onBindView { root.text = it.text }
    }.typeLinker {
        it.type == "type1"
    }
}

fun getTextType2Delegate(): ViewTypeDelegate<TextItem, *> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextType2Binding::inflate
    ) {
        onBindView { root.text = it.text }
    }.typeLinker {
        it.type == "type2"
    }
}

fun ListAdapter<TextItem, *>.submitTextItems(
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