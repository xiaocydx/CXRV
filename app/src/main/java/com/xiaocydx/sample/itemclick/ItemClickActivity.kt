package com.xiaocydx.sample.itemclick

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.*
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import com.xiaocydx.recycler.multitype.listAdapter
import com.xiaocydx.recycler.multitype.register
import com.xiaocydx.sample.*

/**
 * ItemClick示例代码
 *
 * 展示Adapter组合场景、多类型场景下如何设置item点击、长按，设置方式从基础函数开始，逐渐进行简化。
 *
 * @author xcc
 * @date 2022/2/18
 */
class ItemClickActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var listAdapter1: ListAdapter<TextItem, *>
    private lateinit var listAdapter2: ListAdapter<TextItem, *>
    private lateinit var type1Delegate: ViewTypeDelegate<TextItem, *>
    private lateinit var type2Delegate: ViewTypeDelegate<TextItem, *>

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

            listAdapter1 = listAdapter<TextItem> {
                register(type1Delegate)
                register(type2Delegate)
            }.submitTextItems(textPrefix = "ListAdapter1")

            listAdapter2 = listAdapter<TextItem> {
                register(getTextType1Delegate())
                register(getTextType2Delegate())
            }.submitTextItems(textPrefix = "ListAdapter2")

            adapter = ConcatAdapter(
                ConcatAdapter.Config.Builder()
                    .setIsolateViewTypes(false).build(),
                listAdapter1, listAdapter2
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }.also { recyclerView = it }
    }

    private fun setUpItemClick() {
        setUpBaseItemClickByRecyclerView()
        // setUpItemClickByRecyclerView()
        // setUpItemClickByListAdapter()
        // setUpSimpleItemClickByListAdapter()
        // setUpItemClickByViewTypeDelegate()
        // setUpSimpleItemClickByViewTypeDelegate()
    }

    private fun setUpLongItemClick() {
        setUpBaseLongItemClickByRecyclerView()
        // setUpLongItemClickByRecyclerView()
        // setUpLongItemClickByListAdapter()
        // setUpSimpleLongItemClickByListAdapter()
        // setUpLongItemClickByViewTypeDelegate()
        // setUpSimpleLongItemClickByViewTypeDelegate()
    }

    //region 设置item点击
    /**
     * 1.通过RecyclerView设置item点击，最基础的方式
     */
    private fun setUpBaseItemClickByRecyclerView() {
        recyclerView.addOnItemClickListener { itemView ->
            val holder = recyclerView.getChildViewHolder(itemView)
            showToast("点击 bindingAdapterPosition = ${holder.bindingAdapterPosition}")
        }
    }

    /**
     * 2.通过RecyclerView设置item点击，根据Adapter自身特性获取item
     */
    private fun setUpItemClickByRecyclerView() {
        recyclerView.doOnItemClick(listAdapter1) { holder, position ->
            showToast("ListAdapter1 点击 ${getItem(position).text}")
        }
        recyclerView.doOnItemClick(listAdapter2) { holder, position ->
            showToast("ListAdapter2 点击 ${getItem(position).text}")
        }
    }

    /**
     * 3.通过[ListAdapter]设置item点击
     */
    private fun setUpItemClickByListAdapter() {
        listAdapter1.doOnItemClick { holder, item ->
            showToast("ListAdapter1 点击 ${item.text}")
        }
        listAdapter2.doOnItemClick { holder, item ->
            showToast("ListAdapter2 点击 ${item.text}")
        }
    }

    /**
     * 4.通过[ListAdapter]设置item点击，是[setUpItemClickByListAdapter]的简化版本
     */
    private fun setUpSimpleItemClickByListAdapter() {
        listAdapter1.doOnSimpleItemClick { item ->
            showToast("ListAdapter1 点击 ${item.text}")
        }
        listAdapter2.doOnSimpleItemClick { item ->
            showToast("ListAdapter2 点击 ${item.text}")
        }
    }

    /**
     * 5.通过[ViewTypeDelegate]设置item点击
     */
    private fun setUpItemClickByViewTypeDelegate() {
        type1Delegate.doOnItemClick { holder, item ->
            showToast("Type1Delegate 点击 ${item.text}")
        }
        type2Delegate.doOnItemClick { holder, item ->
            showToast("Type2Delegate 点击 ${item.text}")
        }
    }

    /**
     * 6.通过[ViewTypeDelegate]设置item点击，是[setUpItemClickByViewTypeDelegate]的简化版本
     */
    private fun setUpSimpleItemClickByViewTypeDelegate() {
        type1Delegate.doOnSimpleItemClick { item ->
            showToast("Type1Delegate 点击 ${item.text}")
        }
        type2Delegate.doOnSimpleItemClick { item ->
            showToast("Type2Delegate 点击 ${item.text}")
        }
    }
    //endregion

    //region 设置item长按
    /**
     * 1.通过RecyclerView设置item长按，最基础的方式
     */
    private fun setUpBaseLongItemClickByRecyclerView() {
        recyclerView.addOnItemLongClickListener { itemView ->
            val holder = recyclerView.getChildViewHolder(itemView)
            showToast("长按 bindingAdapterPosition = ${holder.bindingAdapterPosition}")
            true
        }
    }

    /**
     * 2.通过RecyclerView设置item长按，根据Adapter自身特性获取item
     */
    private fun setUpLongItemClickByRecyclerView() {
        recyclerView.doOnLongItemClick(listAdapter1) { holder, position ->
            showToast("ListAdapter1 长按 ${getItem(position).text}")
            true
        }
        recyclerView.doOnLongItemClick(listAdapter2) { holder, position ->
            showToast("ListAdapter2 长按 ${getItem(position).text}")
            true
        }
    }

    /**
     * 3.通过[ListAdapter]设置item长按
     */
    private fun setUpLongItemClickByListAdapter() {
        listAdapter1.doOnLongItemClick { holder, item ->
            showToast("ListAdapter1 长按 ${item.text}")
            true
        }
        listAdapter2.doOnLongItemClick { holder, item ->
            showToast("ListAdapter2 长按 ${item.text}")
            true
        }
    }

    /**
     * 4.通过[ListAdapter]设置item长按，是[setUpLongItemClickByListAdapter]的简化版本
     */
    private fun setUpSimpleLongItemClickByListAdapter() {
        listAdapter1.doOnSimpleLongItemClick { item ->
            showToast("ListAdapter1 长按 ${item.text}")
            true
        }
        listAdapter2.doOnSimpleLongItemClick { item ->
            showToast("ListAdapter2 长按 ${item.text}")
            true
        }
    }

    /**
     * 5.通过[ViewTypeDelegate]设置item长按
     */
    private fun setUpLongItemClickByViewTypeDelegate() {
        type1Delegate.doOnLongItemClick { holder, item ->
            showToast("Type1Delegate 长按 ${item.text}")
            true
        }
        type2Delegate.doOnLongItemClick { holder, item ->
            showToast("Type2Delegate 长按 ${item.text}")
            true
        }
    }

    /**
     * 6.通过[ViewTypeDelegate]设置item长按，是[setUpLongItemClickByViewTypeDelegate]的简化版本
     */
    private fun setUpSimpleLongItemClickByViewTypeDelegate() {
        type1Delegate.doOnSimpleLongItemClick { item ->
            showToast("Type1Delegate 长按 ${item.text}")
            true
        }
        type2Delegate.doOnSimpleLongItemClick { item ->
            showToast("Type2Delegate 长按 ${item.text}")
            true
        }
    }
    //endregion
}