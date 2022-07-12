package com.xiaocydx.sample.nested

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.withLayoutParams

/**
 * 嵌套列表示例代码
 *
 * @author xcc
 * @date 2022/4/6
 */
class NestedListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView(): View = RecyclerView(this).apply {
        adapter = VerticalAdapter().apply {
            submitList(getItems(15))
        }
        linear().fixedSize()
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    private fun getItems(
        size: Int
    ): List<VerticalItem> = (1..size).map {
        VerticalItem(
            id = "Vertical-$it",
            title = "List-$it",
            data = (1..size).map { value ->
                HorizontalItem(id = "Horizontal-$value", title = "$it-${value}")
            }
        )
    }
}