package com.xiaocydx.sample.payload

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.payload.Payload

/**
 * [Payload]更新示例代码
 *
 * @author xcc
 * @date 2022/11/13
 */
class PayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = RecyclerView(this)
        .layoutParams(matchParent, matchParent).linear()
        .divider(height = 0.5f.dp) { color(0xFF7E7AAA.toInt()) }
        .adapter(CountAdapter().apply {
            doOnItemClick(target = { binding.btnCount1 }) { holder, _ ->
                holder.setItem { incrementCount1() }
            }
            doOnItemClick(target = { binding.btnCount2 }) { holder, _ ->
                holder.setItem { incrementCount2() }
            }
            doOnItemClick(target = { binding.btnCount3 }) { holder, _ ->
                holder.setItem { incrementCount3() }
            }
            submitList((1..3).map { CountItem(id = it.toString()) })
        })
}