package com.xiaocydx.cxrv.binding

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * 使用[ViewBinding]完成视图绑定的[ViewTypeDelegate]模板类
 *
 * @author xcc
 * @date 2021/12/6
 */
abstract class BindingDelegate<ITEM : Any, VB : ViewBinding> :
        ViewTypeDelegate<ITEM, BindingHolder<VB>>() {
    /**
     * Kotlin中类的函数引用，若不是直接作为内联函数的实参，则会编译为单例，
     * 但此处仍然用属性保存函数引用，不重复获取，即使编译规则改了也不受影响。
     */
    private var inflate: Inflate<VB>? = null

    /**
     * 通过[VB]获取[BindingHolder]
     */
    @Suppress("UNCHECKED_CAST")
    var VB.holder: BindingHolder<VB>
        get() = requireNotNull(
            value = root.getTag(R.id.tag_view_holder) as? BindingHolder<VB>,
            lazyMessage = { "root还未关联ViewHolder" }
        )
        private set(value) {
            root.setTag(R.id.tag_view_holder, value)
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
    protected open fun VB.onCreateView() = Unit

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    protected open fun VB.onBindView(item: ITEM, payloads: List<Any>) = onBindView(item)

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    protected open fun VB.onBindView(item: ITEM) = Unit

    /**
     * 对应[Adapter.onViewRecycled]
     */
    protected open fun VB.onViewRecycled() = Unit

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    protected open fun VB.onFailedToRecycleView(): Boolean = false

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    protected open fun VB.onViewAttachedToWindow() = Unit

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    protected open fun VB.onViewDetachedFromWindow() = Unit

    final override fun onCreateViewHolder(parent: ViewGroup): BindingHolder<VB> {
        if (inflate == null) {
            inflate = inflate()
        }
        val binding = inflate!!(parent.inflater, parent, false)
        val holder = BindingHolder(binding)
        binding.holder = holder
        binding.onCreateView()
        return holder
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