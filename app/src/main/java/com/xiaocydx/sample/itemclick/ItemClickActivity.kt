package com.xiaocydx.sample.itemclick

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import com.xiaocydx.sample.extension.TextItem
import com.xiaocydx.sample.extension.getTextType1Delegate
import com.xiaocydx.sample.extension.getTextType2Delegate
import com.xiaocydx.sample.extension.initMultiTypeTextItems
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.showToast
import com.xiaocydx.sample.withLayoutParams

/**
 * ItemClick示例代码
 *
 * Adapter组合、多类型场景下设置item点击、长按，设置方式从基础函数开始，逐渐进行简化。
 *
 * @author xcc
 * @date 2022/2/18
 */
class ItemClickActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter1: ListAdapter<TextItem, *>
    private lateinit var adapter2: ListAdapter<TextItem, *>
    private lateinit var type1Delegate: BindingDelegate<TextItem, ItemTextType1Binding>
    private lateinit var type2Delegate: BindingDelegate<TextItem, ItemTextType2Binding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        setupItemClick()
        setupLongItemClick()
    }

    private fun contentView(): View = RecyclerView(this).apply {
        recyclerView = this
        type1Delegate = getTextType1Delegate()
        type2Delegate = getTextType2Delegate()

        adapter1 = listAdapter<TextItem> {
            register(type1Delegate)
            register(type2Delegate)
        }.initMultiTypeTextItems()

        adapter2 = listAdapter<TextItem> {
            register(getTextType1Delegate())
            register(getTextType2Delegate())
        }.initMultiTypeTextItems()

        adapter = ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false).build(),
            adapter1, adapter2
        )
        linear().fixedSize()
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    private fun setupItemClick() {
        setupAbsoluteItemClickByRecyclerView()
        // setupRelativeItemClickByRecyclerView()
        // setupItemClickByListAdapter()
        // setupItemClickByViewTypeDelegate()
    }

    private fun setupLongItemClick() {
        setupAbsoluteLongItemClickByRecyclerView()
        // setupRelativeLongItemClickByRecyclerView()
        // setupLongItemClickByListAdapter()
        // setupLongItemClickByViewTypeDelegate()
    }

    //region 设置item点击
    /**
     * 1.通过RecyclerView设置点击，不进行Adapter匹对
     */
    private fun setupAbsoluteItemClickByRecyclerView() {
        recyclerView.doOnItemClick { holder, position ->
            showToast("点击[itemView]\nabsoluteAdapterPosition = $position")
        }
    }

    /**
     * 2.通过RecyclerView设置点击，进行Adapter匹对，并根据Adapter自身特性获取`item`
     */
    private fun setupRelativeItemClickByRecyclerView() {
        recyclerView.doOnItemClick(adapter1) { holder, position ->
            showClickToast(prefix = "Adapter1", view = "itemView", getItem(position), position)
        }
        recyclerView.doOnItemClick(adapter2) { holder, position ->
            showClickToast(prefix = "Adapter2", view = "itemView", getItem(position), position)
        }
    }

    /**
     * 3.通过[ListAdapter]设置点击
     */
    private fun setupItemClickByListAdapter() {
        adapter1.doOnItemClick { holder, item ->
            showClickToast(prefix = "Adapter1", view = "itemView", item, holder.bindingAdapterPosition)
        }
        adapter2.doOnItemClick { holder, item ->
            showClickToast(prefix = "Adapter2", view = "itemView", item, holder.bindingAdapterPosition)
        }
    }

    /**
     * 4.通过[ViewTypeDelegate]设置点击
     */
    private fun setupItemClickByViewTypeDelegate() {
        type1Delegate.doOnItemClick { holder, item ->
            showClickToast(prefix = "Type1Delegate", view = "itemView", item, holder.bindingAdapterPosition)
        }
        type2Delegate.doOnItemClick { holder, item ->
            showClickToast(prefix = "Type2Delegate", view = "itemView", item, holder.bindingAdapterPosition)
        }

        type1Delegate.doOnItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showClickToast(prefix = "Type1Delegate", view = "targetView", item, holder.bindingAdapterPosition)
        }
        type2Delegate.doOnItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showClickToast(prefix = "Type2Delegate", view = "targetView", item, holder.bindingAdapterPosition)
        }
    }
    //endregion

    //region 设置item长按
    /**
     * 1.通过RecyclerView设置长按，不进行Adapter匹对
     */
    private fun setupAbsoluteLongItemClickByRecyclerView() {
        recyclerView.doOnLongItemClick { holder, position ->
            showToast("长按[itemView]\nabsoluteAdapterPosition = $position")
            true
        }
    }

    /**
     * 2.通过RecyclerView设置长按，进行Adapter匹配，并根据Adapter自身特性获取`item`
     */
    private fun setupRelativeLongItemClickByRecyclerView() {
        recyclerView.doOnLongItemClick(adapter1) { holder, position ->
            showLongClickToast(prefix = "Adapter1", view = "itemView", getItem(position), position)
            true
        }
        recyclerView.doOnLongItemClick(adapter2) { holder, position ->
            showLongClickToast(prefix = "Adapter2", view = "itemView", getItem(position), position)
            true
        }
    }

    /**
     * 3.通过[ListAdapter]设置长按
     */
    private fun setupLongItemClickByListAdapter() {
        adapter1.doOnLongItemClick { holder, item ->
            showLongClickToast(prefix = "Adapter1", view = "itemView", item, holder.bindingAdapterPosition)
            true
        }
        adapter2.doOnLongItemClick { holder, item ->
            showLongClickToast(prefix = "Adapter2", view = "itemView", item, holder.bindingAdapterPosition)
            true
        }
    }

    /**
     * 4.通过[ViewTypeDelegate]设置长按
     */
    private fun setupLongItemClickByViewTypeDelegate() {
        type1Delegate.doOnLongItemClick { holder, item ->
            showLongClickToast(prefix = "Type1Delegate", view = "itemView", item, holder.bindingAdapterPosition)
            true
        }
        type2Delegate.doOnLongItemClick { holder, item ->
            showLongClickToast(prefix = "Type2Delegate", view = "itemView", item, holder.bindingAdapterPosition)
            true
        }

        type1Delegate.doOnLongItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showLongClickToast(prefix = "Type1Delegate", view = "targetView", item, holder.bindingAdapterPosition)
            true
        }
        type2Delegate.doOnLongItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showLongClickToast(prefix = "Type2Delegate", view = "targetView", item, holder.bindingAdapterPosition)
            true
        }
    }
    //endregion

    private fun showClickToast(
        prefix: String,
        view: String,
        item: TextItem,
        bindingAdapterPosition: Int = -1
    ) {
        var content = "$prefix 点击[$view] ${item.text}"
        if (bindingAdapterPosition > -1) {
            content += "\nbindingAdapterPosition = $bindingAdapterPosition"
        }
        showToast(content)
    }

    private fun showLongClickToast(
        prefix: String,
        view: String,
        item: TextItem,
        bindingAdapterPosition: Int = -1
    ) {
        var content = "$prefix 长按[$view] ${item.text}"
        if (bindingAdapterPosition > -1) {
            content += "\nbindingAdapterPosition = $bindingAdapterPosition"
        }
        showToast(content)
    }
}