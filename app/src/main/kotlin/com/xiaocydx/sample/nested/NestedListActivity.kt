package com.xiaocydx.sample.nested

import android.os.Bundle
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

    private fun contentView() = RecyclerView(this).apply {
        adapter = OuterAdapter().apply {
            submitList(getItems(15))
        }
        overScrollNever()
        linear().fixedSize()
        withLayoutParams(matchParent, matchParent)
    }

    private fun getItems(size: Int) = (1..size).map {
        val finalSize = if (it % 2 == 0) size / 2 else size
        OuterItem(
            id = "Outer-$it",
            title = "List-$it",
            data = (1..finalSize).map { value ->
                InnerItem(id = "Inner-$value", title = "$it-${value}")
            }
        )
    }
}