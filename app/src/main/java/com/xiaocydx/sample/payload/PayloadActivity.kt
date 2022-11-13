package com.xiaocydx.sample.payload

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.payload.Payload
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.withLayoutParams

/**
 * [Payload]更新示例代码
 *
 * @author xcc
 * @date 2022/11/13
 */
class PayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RecyclerView(this).apply {
            linear().fixedSize().divider {
                height = 0.5f.dp
                color = 0xFF7E7AAA.toInt()
            }
            adapter(CountAdapter().apply {
                doOnItemClick(
                    target = { binding.btnCount1 }
                ) { holder, _ -> holder.setItem { incrementCount1() } }

                doOnItemClick(
                    target = { binding.btnCount2 }
                ) { holder, _ -> holder.setItem { incrementCount2() } }

                doOnItemClick(
                    target = { binding.btnCount3 }
                ) { holder, _ -> holder.setItem { incrementCount3() } }

                submitList((1..3).map { CountItem(id = it.toString()) })
            })
            overScrollNever()
            withLayoutParams(matchParent, matchParent)
        })
    }
}