package com.xiaocydx.sample.itemclick.scenes

import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.emptyDisposable
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding
import com.xiaocydx.sample.extensions.Action
import com.xiaocydx.sample.extensions.TextItem
import com.xiaocydx.sample.extensions.TextType1Delegate
import com.xiaocydx.sample.extensions.TextType2Delegate
import com.xiaocydx.sample.extensions.initMultiTypeTextItems
import com.xiaocydx.sample.indefinite
import com.xiaocydx.sample.snackbar

/**
 * @author xcc
 * @date 2023/3/25
 */
sealed class ItemClickScenes : Action {
    private var disposable = emptyDisposable()
    private val sub1 = Sub(num = 1)
    private val sub2 = Sub(num = 2)
    private val concatAdapter = ConcatAdapter(
        ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false).build(),
        sub1.listAdapter, sub2.listAdapter
    )
    override val text: String
        get() = javaClass.simpleName ?: ""

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

    protected fun RecyclerView.show(content: String) {
        snackbar().setText(content).indefinite().show()
    }

    protected val ViewHolder.targetView: View?
        get() {
            if (this !is BindingHolder<*>) return null
            return when (val binding = binding) {
                is ItemTextType1Binding -> binding.targetView
                is ItemTextType2Binding -> binding.targetView
                else -> null
            }
        }

    protected inner class Sub(val num: Int) {
        val delegate1 = TextType1Delegate()
        val delegate2 = TextType2Delegate()

        val listAdapter = listAdapter<TextItem> {
            register(delegate1)
            register(delegate2)
        }.initMultiTypeTextItems()

        fun show(content: String) {
            listAdapter.recyclerView?.show(content)
        }
    }
}

