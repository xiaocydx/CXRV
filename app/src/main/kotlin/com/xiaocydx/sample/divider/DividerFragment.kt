package com.xiaocydx.sample.divider

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.FooType
import com.xiaocydx.sample.divider.MenuAction.DECREASE_SPAN_COUNT
import com.xiaocydx.sample.divider.MenuAction.INCREASE_SPAN_COUNT
import com.xiaocydx.sample.divider.MenuAction.INSERT_ITEM
import com.xiaocydx.sample.divider.MenuAction.REMOVE_ITEM
import com.xiaocydx.sample.divider.MenuAction.REVERSE_LAYOUT
import com.xiaocydx.sample.divider.MenuAction.REVERSE_ORIENTATION
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2023/10/4
 */
abstract class DividerFragment : Fragment() {
    private val sharedViewModel: DividerSharedViewModel by activityViewModels()
    private var multiTypeFoo = false
    protected lateinit var rvDivider: RecyclerView; private set
    protected lateinit var header: View; private set
    protected lateinit var footer: View; private set

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())
        .apply { rvDivider = this }
        .overScrollNever().fixedSize()
        .layoutParams(matchParent, matchParent)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initCollect()
    }

    @CallSuper
    protected open fun initView() {
        header = createView(isHeader = true)
        footer = createView(isHeader = false)
    }

    private fun initCollect() {
        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                REVERSE_LAYOUT -> reverseLayout()
                REVERSE_ORIENTATION -> reverseOrientation()
                INCREASE_SPAN_COUNT -> increaseSpanCount()
                DECREASE_SPAN_COUNT -> decreaseSpanCount()
                INSERT_ITEM -> insertItem()
                REMOVE_ITEM -> removeItem()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun increaseSpanCount() {
        when (val lm = rvDivider.layoutManager) {
            is GridLayoutManager -> lm.spanCount += 1
            is StaggeredGridLayoutManager -> lm.spanCount += 1
            else -> return
        }
        rvDivider.invalidateItemDecorations()
    }

    private fun decreaseSpanCount() {
        when (val lm = rvDivider.layoutManager) {
            is GridLayoutManager -> if (lm.spanCount > 1) lm.spanCount -= 1
            is StaggeredGridLayoutManager -> if (lm.spanCount > 1) lm.spanCount -= 1
            else -> return
        }
        rvDivider.invalidateItemDecorations()
    }

    private fun reverseLayout() {
        when (val lm = rvDivider.layoutManager) {
            is LinearLayoutManager -> lm.reverseLayout = !lm.reverseLayout
            is StaggeredGridLayoutManager -> lm.reverseLayout = !lm.reverseLayout
            else -> return
        }
        rvDivider.invalidateItemDecorations()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reverseOrientation() {
        val orientation = when (val lm = rvDivider.layoutManager) {
            is LinearLayoutManager -> when (lm.orientation) {
                RecyclerView.VERTICAL -> RecyclerView.HORIZONTAL
                else -> RecyclerView.VERTICAL
            }.also { lm.orientation = it }
            is StaggeredGridLayoutManager -> when (lm.orientation) {
                RecyclerView.VERTICAL -> RecyclerView.HORIZONTAL
                else -> RecyclerView.VERTICAL
            }.also { lm.orientation = it }
            else -> -1
        }
        rvDivider.invalidateItemDecorations()

        val isVertical = orientation == RecyclerView.VERTICAL
        val block: ViewGroup.LayoutParams.() -> Unit = {
            width = if (isVertical) matchParent else 100.dp
            height = if (isVertical) 100.dp else matchParent
        }
        header.updateLayoutParams(block)
        footer.updateLayoutParams(block)
        rvDivider.adapter?.notifyDataSetChanged()
    }

    protected abstract fun insertItem()

    protected abstract fun removeItem()

    protected fun enableMultiTypeFoo() {
        multiTypeFoo = true
    }

    protected fun createFoo(num: Int): Foo {
        val type = when {
            !multiTypeFoo -> FooType.TYPE1
            num % 2 != 0 -> FooType.TYPE1
            else -> FooType.TYPE2
        }
        return Foo(id = num.toString(), name = "Foo-$num", num, "", type)
    }

    private fun createView(isHeader: Boolean) = AppCompatTextView(requireContext()).apply {
        gravity = Gravity.CENTER
        text = if (isHeader) "Header" else "Footer"
        layoutParams(matchParent, 100.dp)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 18.dp.toFloat())
        setBackgroundColor(if (isHeader) 0xFFDBE0FF.toInt() else 0xFFEDDFFF.toInt())
    }
}