package com.xiaocydx.sample.payload

import android.annotation.SuppressLint
import com.xiaocydx.cxrv.binding.BindingAdapter
import com.xiaocydx.cxrv.binding.Inflate
import com.xiaocydx.cxrv.payload.Payload
import com.xiaocydx.cxrv.payload.ifNotEquals
import com.xiaocydx.cxrv.payload.take
import com.xiaocydx.cxrv.payload.value
import com.xiaocydx.sample.databinding.ItemCountBinding

/**
 * @author xcc
 * @date 2022/11/13
 */
@SuppressLint("SetTextI18n")
class CountAdapter : BindingAdapter<CountItem, ItemCountBinding>() {

    override fun inflate(): Inflate<ItemCountBinding> {
        return ItemCountBinding::inflate
    }

    override fun areItemsTheSame(
        oldItem: CountItem,
        newItem: CountItem
    ): Boolean = oldItem.id == newItem.id

    override fun getChangePayload(
        oldItem: CountItem,
        newItem: CountItem
    ): Any = Payload(oldItem, newItem) {
        ifNotEquals { count1 }.add(COUNT1)
        ifNotEquals { count2 }.add(COUNT2)
        // 注释掉count3的比较代码，是为了演示当只有count3不相等时，
        // 若没有添加COUNT3，则按Payload.EMPTY执行兜底更新，
        // 即执行updateCount1().updateCount2().updateCount3()。
        // ifNotEquals { count3 }.add(COUNT3)
    }

    override fun ItemCountBinding.onBindView(
        item: CountItem,
        payloads: List<Any>
    ) = Payload.take(payloads) { value ->
        when (value) {
            COUNT1 -> updateCount1()
            COUNT2 -> updateCount2()
            COUNT3 -> updateCount3()
            else -> updateCount1().updateCount2().updateCount3()
        }
    }

    private fun ItemCountBinding.updateCount1() = apply {
        btnCount1.text = "count1 = ${holder.item.count1}"
    }

    private fun ItemCountBinding.updateCount2() = apply {
        btnCount2.text = "count2 = ${holder.item.count2}"
    }

    private fun ItemCountBinding.updateCount3() = apply {
        btnCount3.text = "count3 = ${holder.item.count3}"
    }

    private companion object {
        val COUNT1 = Payload.value(1)
        val COUNT2 = Payload.value(2)
        val COUNT3 = Payload.value(3)
    }
}