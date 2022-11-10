package com.xiaocydx.sample.viewpager2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.xiaocydx.sample.overScrollNever

/**
 * @author xcc
 * @date 2022/11/10
 */
class FooCategoryAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private var list: MutableList<FooCategory> = mutableListOf()

    fun submitList(newList: List<FooCategory>): Boolean {
        val changed = list != newList
        if (changed) {
            list.clear()
            list.addAll(newList)
            notifyDataSetChanged()
        }
        return changed
    }

    fun getItem(position: Int): FooCategory = list[position]

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long = list[position].id

    override fun containsItem(itemId: Long): Boolean {
        return list.firstOrNull { it.id == itemId } != null
    }

    override fun createFragment(position: Int): Fragment {
        return FooListFragment.newInstance(getItem(position).id)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.overScrollNever()
        recyclerView.itemAnimator = null
    }
}