package com.xiaocydx.sample.itemtouch

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.concat.withFooter
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.itemtouch.addItemTouchCallback
import com.xiaocydx.cxrv.itemtouch.itemTouch
import com.xiaocydx.cxrv.itemtouch.onDragMoveItem
import com.xiaocydx.cxrv.itemtouch.onSwipeRemoveItem
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.databinding.ItemTextTypeBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.extensions.TextItem
import com.xiaocydx.sample.extensions.getTextType1Delegate
import com.xiaocydx.sample.extensions.getTextType2Delegate
import com.xiaocydx.sample.extensions.initMultiTypeTextItems
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever

/**
 * ItemTouch示例代码
 *
 * @author xcc
 * @date 2023/3/5
 */
class ItemTouchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = RecyclerView(this)
        .layoutParams(matchParent, matchParent)
        .overScrollNever().linear().fixedSize()
        .adapter(listAdapter<TextItem> {
            register(getTextType1Delegate())
            register(getTextType2Delegate())
            listAdapter.initItemTouch()
            listAdapter.initMultiTypeTextItems()
        }.withHeaderFooter())

    /**
     * ItemTouch的配置方式有三种：
     * 1. [RecyclerView.addItemTouchCallback]。
     * 2. [RecyclerView.itemTouch]。
     * 3. [ListAdapter.itemTouch]。
     *
     * [RecyclerView.addItemTouchCallback]是ItemTouch的基础配置方式，
     * [RecyclerView.itemTouch]在[RecyclerView.addItemTouchCallback]的基础上简化配置代码，
     * [ListAdapter.itemTouch]在[RecyclerView.itemTouch]的基础上进一步简化配置代码。
     *
     * 该函数展示了如何通过[ListAdapter.itemTouch]完成ItemTouch的配置。
     */
    private fun ListAdapter<TextItem, *>.initItemTouch() = itemTouch {
        // 拖动时移动item，结合ListAdapter自身特性的简化函数
        onDragMoveItem()
        // 侧滑时移除item，结合ListAdapter自身特性的简化函数
        onSwipeRemoveItem()
        // ACTION_DOWN触摸到targetView就能开始拖动，
        // withLongPress = true表示继续启用长按itemView拖动。
        startDragView(withLongPress = true) { holder ->
            holder.let { it as? BindingHolder<*> }
                ?.let { it.binding as? ItemTextTypeBinding }
                ?.targetView ?: holder.itemView
        }
        // 拖动开始时放大itemView
        onSelected {
            it.itemView.scaleX = 1.1f
            it.itemView.scaleY = 1.1f
        }
        // 拖动结束时恢复itemView
        clearView {
            it.itemView.scaleX = 1.0f
            it.itemView.scaleY = 1.0f
        }
    }

    /**
     * 连接Header和Footer，拖动时移动item不会跟Header和Footer交换位置
     */
    private fun ListAdapter<TextItem, *>.withHeaderFooter(): Adapter<*> {
        return withHeader(createView(isHeader = true).toAdapter())
            .withFooter(createView(isHeader = false).toAdapter())
    }

    private fun createView(isHeader: Boolean) = View(this).apply {
        layoutParams(matchParent, 100.dp)
        setBackgroundColor(if (isHeader) 0xFF92C3FF.toInt() else 0xFF958CFF.toInt())
    }
}