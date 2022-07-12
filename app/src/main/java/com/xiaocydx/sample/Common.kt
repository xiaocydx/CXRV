package com.xiaocydx.sample

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.annotation.Px
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max

/**
 * [TypedValue.complexToDimensionPixelSize]的舍入逻辑，
 * 用于确保[dp]转换的px值，和xml解析转换的px值一致。
 */
@Px
private fun Float.toRoundingPx(): Int {
    return (if (this >= 0) this + 0.5f else this - 0.5f).toInt()
}

@get:Px
val Int.dp: Int
    get() = toFloat().dp

@get:Px
val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toRoundingPx()

val Fragment.viewLifecycle: Lifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope: CoroutineScope
    get() = viewLifecycleOwner.lifecycleScope

inline fun Lifecycle.doOnStateChanged(
    targetState: Lifecycle.State,
    once: Boolean = true,
    crossinline action: (Lifecycle) -> Unit
) {
    addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val lifecycle = source.lifecycle
            if (lifecycle.currentState !== targetState) return
            if (once) lifecycle.removeObserver(this)
            action(lifecycle)
        }
    })
}

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Fragment.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(text, duration)
}

@Suppress("unused")
inline val View.matchParent: Int
    get() = ViewGroup.LayoutParams.MATCH_PARENT

@Suppress("unused")
inline val View.wrapContent: Int
    get() = ViewGroup.LayoutParams.WRAP_CONTENT

fun View.overScrollNever() {
    overScrollMode = View.OVER_SCROLL_NEVER
}

inline fun View.withLayoutParams(width: Int, height: Int, block: MarginLayoutParams.() -> Unit = {}) {
    layoutParams = MarginLayoutParams(width, height).apply(block)
}

inline fun View.onClick(crossinline block: () -> Unit) {
    setOnClickListener { block() }
}

inline fun ViewPager2.registerOnPageChangeCallback(
    crossinline onScrolled: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit = { _, _, _ -> },
    crossinline onSelected: (position: Int) -> Unit = {},
    crossinline onScrollStateChanged: (state: Int) -> Unit = {}
): ViewPager2.OnPageChangeCallback {
    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            onScrolled(position, positionOffset, positionOffsetPixels)
        }

        override fun onPageSelected(position: Int) {
            onSelected(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            onScrollStateChanged(state)
        }
    }
    registerOnPageChangeCallback(callback)
    return callback
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