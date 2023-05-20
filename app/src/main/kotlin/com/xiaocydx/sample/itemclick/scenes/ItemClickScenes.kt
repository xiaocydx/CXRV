package com.xiaocydx.sample.itemclick.scenes

import android.widget.Toast
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.emptyDisposable
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.databinding.ItemTextTypeBinding
import com.xiaocydx.sample.extensions.TextItem
import com.xiaocydx.sample.extensions.getTextType1Delegate
import com.xiaocydx.sample.extensions.getTextType2Delegate
import com.xiaocydx.sample.extensions.initMultiTypeTextItems
import com.xiaocydx.sample.showToast

/**
 * @author xcc
 * @date 2023/3/25
 */
sealed class ItemClickScenes {
    private var disposable = emptyDisposable()
    private val sub1 = Sub(num = 1)
    private val sub2 = Sub(num = 2)
    private val concatAdapter = ConcatAdapter(
        ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false).build(),
        sub1.listAdapter, sub2.listAdapter
    )
    val text = javaClass.simpleName ?: ""

    fun apply(rv: RecyclerView): Disposable {
        disposable = emptyDisposable()
        setup(rv, sub1, sub2)
        rv.adapter = concatAdapter
        return disposable
    }

    protected abstract fun setup(rv: RecyclerView, sub1: Sub, sub2: Sub)

    protected fun Disposable.autoDispose() {
        disposable += this
    }

    protected fun RecyclerView.toast(content: String) {
        context.showToast(content, Toast.LENGTH_LONG)
    }

    protected inner class Sub(val num: Int) {
        val delegate1 = getTextType1Delegate()
        val delegate2 = getTextType2Delegate()

        val listAdapter = listAdapter<TextItem> {
            register(delegate1)
            register(delegate2)
        }.initMultiTypeTextItems().ensureVH()

        fun toast(content: String) {
            listAdapter.recyclerView?.toast(content)
        }

        /**
         * 示例代码的`VH`类型唯一，因此进行类型转换，让[ItemClickScenes]子类的逻辑清晰一些
         */
        @Suppress("UNCHECKED_CAST")
        private fun ListAdapter<TextItem, *>.ensureVH() = let {
            it as ListAdapter<TextItem, BindingHolder<ItemTextTypeBinding>>
        }
    }
}

