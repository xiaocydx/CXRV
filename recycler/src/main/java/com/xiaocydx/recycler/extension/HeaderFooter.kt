package com.xiaocydx.recycler.extension

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.xiaocydx.recycler.widget.ViewAdapter

/**
 * 添加[header]，并将当前adapter设为[ConcatAdapter]
 *
 * **注意**：在添加[header]前，先设置Adapter，否则抛出[IllegalArgumentException]。
 */
fun <T : RecyclerView> T.addHeader(header: View): T {
    val adapter: Adapter<*> = requireNotNull(adapter) {
        "请先对RecyclerView设置Adapter，然后再添加Header"
    }
    if (adapter is ConcatAdapter) {
        adapter.adapters.firstOrNull {
            it.containView(header)
        }?.let { return this }
        adapter.addAdapter(0, header.toAdapter())
        if (isFirstItemCompletelyVisible) {
            scrollToPosition(0)
        }
    } else {
        setAdapter(ConcatAdapter(DEFAULT_CONFIG, header.toAdapter(), adapter))
    }
    return this
}

/**
 * 添加[footer]，并将当前adapter设为[ConcatAdapter]
 *
 * **注意**：在添加[footer]前，先设置Adapter，否则抛出[IllegalArgumentException]。
 */
fun <T : RecyclerView> T.addFooter(footer: View): T {
    val adapter: Adapter<*> = requireNotNull(adapter) {
        "请先对RecyclerView设置Adapter，然后再添加Footer"
    }
    if (adapter is ConcatAdapter) {
        adapter.adapters.lastOrNull {
            it.containView(footer)
        }?.let { return this }
        adapter.addAdapter(footer.toAdapter())
    } else {
        setAdapter(ConcatAdapter(DEFAULT_CONFIG, adapter, footer.toAdapter()))
    }
    return this
}

/**
 * 若当前适配器为[ConcatAdapter]且包含[header]，则移除[header]
 *
 * @return 返回[header]转换后的[ViewAdapter]，若[header]未添加过或已移除过，则返回null。
 */
fun RecyclerView.removeHeader(header: View): ViewAdapter<*>? {
    val concatAdapter = adapter as? ConcatAdapter ?: return null
    val headerAdapter: Adapter<*>? = concatAdapter.adapters
        .firstOrNull { adapter -> adapter.containView(header) }
        ?.also { concatAdapter.removeAdapter(it) }
    return headerAdapter as? ViewAdapter<*>
}

/**
 * 若当前适配器为[ConcatAdapter]且包含[footer]，则移除[footer]
 *
 * @return 返回[footer]转换后的[ViewAdapter]，若[footer]未添加过或已移除过，则返回null。
 */
fun RecyclerView.removeFooter(footer: View): ViewAdapter<*>? {
    val concatAdapter = adapter as? ConcatAdapter ?: return null
    val footerAdapter: Adapter<*>? = concatAdapter.adapters
        .lastOrNull { adapter -> adapter.containView(footer) }
        ?.also { concatAdapter.removeAdapter(it) }
    return footerAdapter as? ViewAdapter<*>
}

/**
 * 按[header]、当前适配器的顺序连接为[ConcatAdapter]
 */
@CheckResult
fun Adapter<*>.withHeader(
    header: ViewAdapter<*>
): ConcatAdapter = when (this) {
    is ConcatAdapter -> apply { addAdapter(0, header) }
    else -> ConcatAdapter(DEFAULT_CONFIG, header, this)
}

/**
 * 按当前适配器、[footer]的顺序连接为[ConcatAdapter]
 */
@CheckResult
fun Adapter<*>.withFooter(
    footer: ViewAdapter<*>
): ConcatAdapter = when (this) {
    is ConcatAdapter -> apply { addAdapter(footer) }
    else -> ConcatAdapter(DEFAULT_CONFIG, this, footer)
}

/**
 * 若配置隔离ViewType，则在共享[RecycledViewPool]的场景下，
 * Header和Footer的隔离ViewType会导致获取已回收的ViewHolder抛出异常。
 *
 * ### 场景描述
 * FragmentA和FragmentB共享[RecycledViewPool]，并且内容区Adapter相同。
 *
 * FragmentA有一个Header和内容区Adapter，
 * FragmentA开启了隔离ViewType配置:
 * * Header的隔离ViewType = 0。
 * * 内容区itemView的隔离ViewType = 1。
 *
 * FragmentB只有内容区Adapter，itemView的ViewType = 0，
 * 从[RecycledViewPool]中可能会取出Header的ViewHolder，
 * Header的ViewHolder和内容区的ViewHolder类型不一致，导致抛出类型转换异常。
 *
 * ### 解决方案
 * Header和Footer默认不使用隔离ViewType配置，而是将[View.hashCode]作为ViewType。
 */
private val DEFAULT_CONFIG: ConcatAdapter.Config =
        ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false)
            .build()

/**
 * 将[View]转换为适配器，用于[ConcatAdapter]的连接场景
 */
fun View.toAdapter(): ViewAdapter<*> = SimpleViewAdapter(this)

private fun Adapter<*>.containView(view: View): Boolean {
    return this is SimpleViewAdapter && this.view == view
}

@VisibleForTesting
internal class SimpleViewAdapter(
    val view: View
) : ViewAdapter<ViewAdapter.ViewHolder>(currentAsItem = true) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(view).resolveLayoutParams(parent)
    }

    override fun getItemViewType(): Int = view.hashCode()
}