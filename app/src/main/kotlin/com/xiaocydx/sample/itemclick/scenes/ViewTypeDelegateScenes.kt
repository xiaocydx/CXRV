package com.xiaocydx.sample.itemclick.scenes

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.sample.databinding.ItemTextTypeBinding
import com.xiaocydx.sample.extension.TextItem

/**
 * 通过[ViewTypeDelegate]设置点击和长按`action`
 *
 * @author xcc
 * @date 2023/3/25
 */
class ViewTypeDelegateScenes : ItemClickScenes() {
    private var isSetup = false

    /**
     * 设置点击和长按`action`
     *
     * 该设置方式提供`ViewHolder`和`item`，返回[Disposable]，没用[autoDispose]的原因：
     * 1. 当[ListAdapter]从RecyclerView分离时，会移除点击和长按`action`，
     * 2. 当[ListAdapter]附加到RecyclerView时，会设置回点击和长按`action`。
     * 切换到其它[ItemClickScenes]执行1，切换回当前[ItemClickScenes]执行2。
     */
    override fun setup(rv: RecyclerView, sub1: Sub, sub2: Sub) {
        if (isSetup) return
        isSetup = true
        sub1.setup()
        sub2.setup()
    }

    private fun Sub.setup() {
        setup(dNum = 1, delegate1)
        setup(dNum = 2, delegate2)
    }

    private fun Sub.setup(
        dNum: Int,
        delegate: BindingDelegate<TextItem, ItemTextTypeBinding>
    ) {
        delegate.doOnItemClick { holder, item ->
            toast(dNum, "点击", "itemView", holder, item)
        }
        delegate.doOnLongItemClick { holder, item ->
            toast(dNum, "长按", "itemView", holder, item)
            true
        }

        delegate.doOnItemClick(target = { binding.targetView }) { holder, item ->
            toast(dNum, "点击", "targetView", holder, item)
        }
        delegate.doOnLongItemClick(target = { binding.targetView }) { holder, item ->
            toast(dNum, "长按", "targetView", holder, item)
            true
        }
    }

    private fun Sub.toast(dNum: Int, action: String, view: String, holder: ViewHolder, item: TextItem) {
        toast("$text\n" +
                "$action $view\n" +
                "item.text = ${item.text}\n" +
                "viewTypeDelegate = Delegate$dNum\n" +
                "bindingAdapter = ListAdapter${num}\n" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}")
    }
}