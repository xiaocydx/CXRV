package com.xiaocydx.sample.viewpager2

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.xiaocydx.cxrv.itemclick.doOnItemClick
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
class FooListAdapter(
    /**
     * 当前[FooListFragment]会复用其它[FooListFragment]回收进`sharedRecycledViewPool`的[ImageView]，
     * [Glide]对被共享复用的[ImageView]再次加载图片时，未及时移除上一个[RequestManager]记录的[Target]，
     * 当上一个[RequestManager.onDestroy]被调用时，`clear(Target)`对被共享复用的[ImageView]设置占位图。
     */
    private val fragment: FooListFragment
) : ListAdapter<Foo, RecyclerView.ViewHolder>() {

    /**
     * 解决方案1
     *
     * 将[RequestManager]跟[FooListFragment]的父级关联，规避未及时移除[Target]造成的问题。
     * 因为此处示例代码比较简单，没有加载GIF、WEBP动图，所以选择解决方案1进行演示。
     *
     * **注意**：跟父级关联后，[FooListFragment]销毁时不会取消请求，而是在[onBindViewHolder]中，
     * 对被共享复用的[ImageView]再次加载图片时，若请求不一致，则取消上一次的请求，
     * 若业务场景不能接受这一点，则可以选择解决方案2。
     */
    private val requestManager = when (fragment.parentFragment) {
        null -> Glide.with(fragment.requireActivity())
        else -> Glide.with(fragment.requireParentFragment())
    }

    /**
     * 将上面的代码替换为：`val requestManager = Glide.with(fragment)`，
     * 将[ViewPager2]从1滚动到6，再滚动回2，能观察到被共享复用的[ImageView]设置占位图的现象
     */
    // private val requestManager = Glide.with(fragment)

    /**
     * [doOnItemClick]会在合适的时机会清除状态，
     * 避免`sharedRecycledViewPool`场景出现内存泄漏问题。
     * 或者不使用 [doOnItemClick]，而是在[onBindViewHolder]设置[OnClickListener]，
     * 在[onViewRecycled]置空[OnClickListener]，确保逻辑对称，避免内存泄漏问题。
     */
    init {
        doOnItemClick { holder, item ->
            val context = holder.itemView.context
            context.showToast("doOnItemClick ${fragment.TAG}-${item.name}")
        }
    }

    /**
     * 对`itemView`设置的[OnClickListener]会捕获外部[FooListFragment]（访问了TAG），
     * 当RecyclerView从Window上分离时，`itemView`会被回收进`sharedRecycledViewPool`,
     * 这会间接导致已销毁的[FooListFragment]法被GC，即出现内存泄漏问题。
     *
     * 注释`init {}`中的[doOnItemClick]，恢复`itemView.setOnClickListener {}`，
     * 再滚动[ViewPager2]，让[FooListFragment]被销毁，等待一段时间后，就能看到[LeakCanary]的内存泄漏提示。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = FooItemView(parent.context)
        val holder = object : RecyclerView.ViewHolder(itemView) {}
        Log.e(fragment.TAG, "onCreateView：${System.currentTimeMillis()}")
        // itemView.setOnClickListener {
        //     val context = holder.itemView.context
        //     context.showToast("setOnClickListener ${fragment.TAG}-${holder.item.name}")
        // }
        return holder
    }

    /**
     * 将[RequestBuilder.into]替换为[RequestBuilder.intoIsolate]，
     * 能解决调用`GifDrawable.stop()`、`WebpDrawable.stop()`停止动图，
     * 出现相同动图url的[ImageView]内容绘制混乱的问题。
     *
     * [RequestBuilder.intoIsolate]做的事：
     * 1. 对缓存键混入附带categoryId的`signature`。
     * 2. 继承[ImageViewTarget]，重写`equals()`和`hashCode()`。
     * 因为对缓存键混入了`signature`，所以对被共享复用的[ImageView]再次加载图片时，
     * 即使url跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
     *
     * **注意**：由于对缓存键混入了`signature`，并且重写了[ImageViewTarget]的`equals()`和`hashCode()`,
     * 因此[RequestBuilder.intoIsolate]也能解决上面提到的未及时清除[Target]造成的问题，可作为解决方案2。
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: Foo
    ): Unit = with(holder.itemView as FooItemView) {
        textView.text = item.name
        requestManager
            .load(item.url).centerCrop()
            .placeholder(R.color.placeholder_color)
            .into(imageView)
        // .intoIsolate(imageView, categoryId)
    }

    override fun areItemsTheSame(oldItem: Foo, newItem: Foo): Boolean {
        return oldItem.id == newItem.id
    }
}