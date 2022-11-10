package com.xiaocydx.sample.viewpager2

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.sample.*
import com.xiaocydx.sample.paging.Foo

/**
 * 讲述`sharedRecycledViewPool`场景的一些注意事项：
 * 1. 除了有视图设置逻辑，还要有视图重置逻辑，逻辑对称才能避免内存泄漏问题。
 * 2. 若使用[Glide]对[ImageView]加载图片，则需要和父级关联或者做另外处理。
 *
 * @author xcc
 * @date 2022/8/6
 */
@Suppress("KDocUnresolvedReference")
class FooListAdapterError(
    /**
     * 当前[FooListFragment]会复用其它[FooListFragment]回收进`sharedRecycledViewPool`的[ImageView]，
     * [Glide]对被共享复用的[ImageView]再次加载图片时，未及时移除上一个[RequestManager]记录的[Target]，
     * 当上一个[RequestManager.onDestroy]被调用时，`clear(Target)`对被共享复用的[ImageView]设置占位图。
     */
    private val fragment: FooListFragment
) : ListAdapter<Foo, FooViewHolder>() {

    /**
     * 1. 将[ViewPager2]从1滚动到10，等待每一页加载完，再滚动回9，
     * 能观察到被共享复用的[ImageView]设置占位图的现象。
     */
    private val requestManager = Glide.with(fragment)

    /**
     * 2. 对`itemView`设置的[OnClickListener]会捕获外部[FooListFragment]（访问了TAG），
     * 当RecyclerView从Window上分离时，`itemView`会被回收进`sharedRecycledViewPool`,
     * 这会间接导致已销毁的[FooListFragment]法被GC，即出现内存泄漏问题。
     *
     * 滚动[ViewPager2]，让[FooListFragment]被销毁，等待一段时间后，就能看到[LeakCanary]的内存泄漏提示。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooViewHolder {
        val itemView = FooItemView(parent.context)
        val holder = FooViewHolder(itemView)
        Log.e(fragment.TAG, "onCreateView：${System.currentTimeMillis()}")
        itemView.setOnClickListener {
            val context = holder.itemView.context
            context.showToast("setOnClickListener ${fragment.TAG}-${holder.item.name}")
        }
        return holder
    }

    /**
     * 3. 调用`GifDrawable.stop()`、`WebpDrawable.stop()`停止动图，
     * 出现相同动图url的[ImageView]内容绘制混乱的问题。
     */
    override fun onBindViewHolder(
        holder: FooViewHolder,
        item: Foo
    ): Unit = with(holder.itemView as FooItemView) {
        textView.text = item.name
        requestManager.load(item.url).centerCrop()
            .placeholder(R.color.placeholder_color)
            .into(imageView)
    }

    override fun areItemsTheSame(oldItem: Foo, newItem: Foo): Boolean {
        return oldItem.id == newItem.id
    }
}