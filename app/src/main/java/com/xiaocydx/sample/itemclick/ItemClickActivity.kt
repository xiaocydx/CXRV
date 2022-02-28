package com.xiaocydx.sample.itemclick

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.binding.BindingDelegate
import com.xiaocydx.recycler.extension.*
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import com.xiaocydx.recycler.multitype.listAdapter
import com.xiaocydx.recycler.multitype.register
import com.xiaocydx.sample.*
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding

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
        setUpItemClick()
        setUpLongItemClick()
    }

    private fun contentView(): View {
        return RecyclerView(this).apply {
            linear()
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
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }.also { recyclerView = it }
    }

    private fun setUpItemClick() {
        setUpItemClickByRecyclerView()
        // setUpItemClickByListAdapter()
        // setUpSimpleItemClickByListAdapter()
        // setUpItemClickByViewTypeDelegate()
        // setUpSimpleItemClickByViewTypeDelegate()
    }

    private fun setUpLongItemClick() {
        setUpLongItemClickByRecyclerView()
        // setUpLongItemClickByListAdapter()
        // setUpSimpleLongItemClickByListAdapter()
        // setUpLongItemClickByViewTypeDelegate()
        // setUpSimpleLongItemClickByViewTypeDelegate()
    }

    //region 设置item点击
    /**
     * 1.通过RecyclerView设置item点击，根据Adapter自身特性获取item
     */
    private fun setUpItemClickByRecyclerView() {
        recyclerView.doOnItemClick(adapter1) { holder, position ->
            showItemViewClickToast(prefix = "Adapter1", getItem(position))
        }
        recyclerView.doOnItemClick(adapter2) { holder, position ->
            showItemViewClickToast(prefix = "Adapter2", getItem(position))
        }
    }

    /**
     * 2.通过[ListAdapter]设置item点击
     */
    private fun setUpItemClickByListAdapter() {
        adapter1.doOnItemClick { holder, item ->
            showItemViewClickToast(prefix = "Adapter1", item)
        }
        adapter2.doOnItemClick { holder, item ->
            showItemViewClickToast(prefix = "Adapter2", item)
        }
    }

    /**
     * 3.通过[ListAdapter]设置item点击，是[setUpItemClickByListAdapter]的简化版本
     */
    private fun setUpSimpleItemClickByListAdapter() {
        adapter1.doOnSimpleItemClick { item ->
            showItemViewClickToast(prefix = "Adapter1", item)
        }
        adapter2.doOnSimpleItemClick { item ->
            showItemViewClickToast(prefix = "Adapter2", item)
        }
    }

    /**
     * 4.通过[ViewTypeDelegate]设置item点击
     */
    private fun setUpItemClickByViewTypeDelegate() {
        type1Delegate.doOnItemClick { holder, item ->
            showItemViewClickToast(prefix = "Type1Delegate", item)
        }
        type2Delegate.doOnItemClick { holder, item ->
            showItemViewClickToast(prefix = "Type2Delegate", item)
        }

        type1Delegate.doOnItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showTargetViewClickToast(prefix = "Type1Delegate", item)
        }
        type2Delegate.doOnItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showTargetViewClickToast(prefix = "Type2Delegate", item)
        }
    }

    /**
     * 5.通过[ViewTypeDelegate]设置item点击，是[setUpItemClickByViewTypeDelegate]的简化版本
     */
    private fun setUpSimpleItemClickByViewTypeDelegate() {
        type1Delegate.doOnSimpleItemClick { item ->
            showItemViewClickToast(prefix = "Type1Delegate", item)
        }
        type2Delegate.doOnSimpleItemClick { item ->
            showItemViewClickToast(prefix = "Type2Delegate", item)
        }
    }
    //endregion

    //region 设置item长按
    /**
     * 1.通过RecyclerView设置item长按，根据Adapter自身特性获取item
     */
    private fun setUpLongItemClickByRecyclerView() {
        recyclerView.doOnLongItemClick(adapter1) { holder, position ->
            showItemViewLongClickToast(prefix = "Adapter1", getItem(position))
            true
        }
        recyclerView.doOnLongItemClick(adapter2) { holder, position ->
            showItemViewLongClickToast(prefix = "Adapter2", getItem(position))
            true
        }
    }

    /**
     * 2.通过[ListAdapter]设置item长按
     */
    private fun setUpLongItemClickByListAdapter() {
        adapter1.doOnLongItemClick { holder, item ->
            showItemViewLongClickToast(prefix = "Adapter1", item)
            true
        }
        adapter2.doOnLongItemClick { holder, item ->
            showItemViewLongClickToast(prefix = "Adapter2", item)
            true
        }
    }

    /**
     * 3.通过[ListAdapter]设置item长按，是[setUpLongItemClickByListAdapter]的简化版本
     */
    private fun setUpSimpleLongItemClickByListAdapter() {
        adapter1.doOnSimpleLongItemClick { item ->
            showItemViewLongClickToast(prefix = "Adapter1", item)
            true
        }
        adapter2.doOnSimpleLongItemClick { item ->
            showItemViewLongClickToast(prefix = "Adapter2", item)
            true
        }
    }

    /**
     * 4.通过[ViewTypeDelegate]设置item长按
     */
    private fun setUpLongItemClickByViewTypeDelegate() {
        type1Delegate.doOnLongItemClick { holder, item ->
            showItemViewLongClickToast(prefix = "Type1Delegate", item)
            true
        }
        type2Delegate.doOnLongItemClick { holder, item ->
            showItemViewLongClickToast(prefix = "Type2Delegate", item)
            true
        }

        type1Delegate.doOnLongItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showTargetViewLongClickToast(prefix = "Type1Delegate", item)
            true
        }
        type2Delegate.doOnLongItemClick(
            target = { binding.targetView }
        ) { holder, item ->
            showTargetViewLongClickToast(prefix = "Type2Delegate", item)
            true
        }
    }

    /**
     * 5.通过[ViewTypeDelegate]设置item长按，是[setUpLongItemClickByViewTypeDelegate]的简化版本
     */
    private fun setUpSimpleLongItemClickByViewTypeDelegate() {
        type1Delegate.doOnSimpleLongItemClick { item ->
            showItemViewLongClickToast(prefix = "Type1Delegate", item)
            true
        }
        type2Delegate.doOnSimpleLongItemClick { item ->
            showItemViewLongClickToast(prefix = "Type2Delegate", item)
            true
        }
    }
    //endregion

    private fun showItemViewClickToast(prefix: String, item: TextItem) {
        showToast("$prefix 点击itemView \nitem.text = ${item.text}")
    }

    private fun showTargetViewClickToast(prefix: String, item: TextItem) {
        showToast("$prefix 点击targetView \nitem.text = ${item.text}")
    }

    private fun showItemViewLongClickToast(prefix: String, item: TextItem) {
        showToast("$prefix 长按itemView \nitem.text = ${item.text}")
    }

    private fun showTargetViewLongClickToast(prefix: String, item: TextItem) {
        showToast("$prefix 长按targetView \nitem.text = ${item.text}")
    }
}