package com.xiaocydx.recycler.binding

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.holder
import androidx.viewbinding.ViewBinding
import com.xiaocydx.recycler.list.ListAdapter

/**
 * 使用[ViewBinding]完成视图绑定的[ListAdapter]模板类
 *
 * @author xcc
 * @date 2021/12/5
 */
abstract class BindingAdapter<ITEM : Any, VB : ViewBinding> : ListAdapter<ITEM, BindingHolder<VB>>() {
    /**
     * Kotlin中类的函数引用，若不是直接作为内联函数的实参，则会编译为单例，
     * 但此处仍然用属性保存函数引用，不重复获取，即使编译规则改了也不受影响。
     */
    private var inflate: Inflate<VB>? = null

    /**
     * 通过[VB]获取[BindingHolder]
     */
    @Suppress("UNCHECKED_CAST")
    protected val VB.holder: BindingHolder<VB>
        get() = requireNotNull(root.holder as? BindingHolder<VB>) {
            "没有跟root关联的ViewHolder，请在onCreateView()之后获取ViewHolder。"
        }

    /**
     * 函数引用`VB::inflate`
     */
    protected abstract fun inflate(): Inflate<VB>

    /**
     * 对应[Adapter.onCreateViewHolder]
     *
     * 可以在该函数中完成初始化工作，例如设置点击监听。
     */
    protected open fun VB.onCreateView(): Unit = Unit

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    protected open fun VB.onBindView(item: ITEM, payloads: List<Any>) {
        onBindView(item)
    }

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    protected open fun VB.onBindView(item: ITEM): Unit = Unit

    /**
     * 对应[Adapter.onViewRecycled]
     */
    protected open fun VB.onViewRecycled(): Unit = Unit

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    protected open fun VB.onFailedToRecycleView(): Boolean = false

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    protected open fun VB.onViewAttachedToWindow(): Unit = Unit

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    protected open fun VB.onViewDetachedFromWindow(): Unit = Unit

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<VB> {
        if (inflate == null) {
            inflate = inflate()
        }
        val binding = inflate!!(parent.inflater, parent, false)
        binding.onCreateView()
        return BindingHolder(binding)
    }

    final override fun onBindViewHolder(holder: BindingHolder<VB>, item: ITEM, payloads: List<Any>) {
        holder.binding.onBindView(item, payloads)
    }

    final override fun onBindViewHolder(holder: BindingHolder<VB>, item: ITEM) {
        holder.binding.onBindView(item)
    }

    final override fun onViewRecycled(holder: BindingHolder<VB>) {
        holder.binding.onViewRecycled()
    }

    final override fun onFailedToRecycleView(holder: BindingHolder<VB>): Boolean {
        return holder.binding.onFailedToRecycleView()
    }

    final override fun onViewAttachedToWindow(holder: BindingHolder<VB>) {
        super.onViewAttachedToWindow(holder)
        holder.binding.onViewAttachedToWindow()
    }

    final override fun onViewDetachedFromWindow(holder: BindingHolder<VB>) {
        holder.binding.onViewDetachedFromWindow()
    }
}