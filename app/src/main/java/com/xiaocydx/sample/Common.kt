package com.xiaocydx.sample

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.Toast
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xiaocydx.recycler.binding.BindingDelegate
import com.xiaocydx.recycler.binding.bindingDelegate
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.submitList
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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

val Fragment.viewLifecycle: Lifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope: CoroutineScope
    get() = viewLifecycleOwner.lifecycleScope

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Fragment.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(text, duration)
}

fun ComponentActivity.launchRepeatOnLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
): Job = lifecycleScope.launch(context, start) {
    repeatOnLifecycle(state, block)
}

fun Fragment.launchRepeatOnViewLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
): Job = viewLifecycleScope.launch(context, start) {
    viewLifecycle.repeatOnLifecycle(state, block)
}

data class TextItem(val text: String, val type: String)

fun getTextType1Delegate(): BindingDelegate<TextItem, ItemTextType1Binding> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextType1Binding::inflate
    ) {
        typeLinker { it.type == "type1" }
        onBindView { textView.text = it.text }
    }
}

fun getTextType2Delegate(): BindingDelegate<TextItem, ItemTextType2Binding> {
    return bindingDelegate(
        uniqueId = TextItem::text,
        inflate = ItemTextType2Binding::inflate
    ) {
        typeLinker { it.type == "type2" }
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