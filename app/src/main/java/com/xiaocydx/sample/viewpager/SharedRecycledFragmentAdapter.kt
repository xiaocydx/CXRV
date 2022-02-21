package com.xiaocydx.sample.viewpager

import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * @author xcc
 * @date 2022/2/21
 */
abstract class SharedRecycledFragmentAdapter(
    fm: FragmentManager,
    sharedPool: RecycledViewPool = RecycledViewPool()
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val controller = SharedPoolController(fm, sharedPool)

    abstract override fun getItem(position: Int): SharedRecycledFragment

    @CallSuper
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val viewPager = container as ViewPager
        if (!controller.isBindViewPager) {
            controller.bindViewPager(viewPager)
        }
        return super.instantiateItem(container, position).also {
            val isCurrent = viewPager.currentItem == position
            controller.instantiateItem(it as Fragment, isCurrent)
        }
    }

    @CallSuper
    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        super.destroyItem(container, position, item)
        controller.destroyItem(item as Fragment)
    }

    private class AttachAction private constructor(
        private val fragmentRef: WeakReference<Fragment>,
        val isCurrent: Boolean,
        val isPreset: Boolean,
        var isLoaded: Boolean
    ) {
        val key = getKey(requireNotNull(fragmentRef.get()))
        val fragment: Fragment?
            get() = fragmentRef.get()

        constructor(
            fragment: Fragment,
            isCurrent: Boolean = false,
            isPreset: Boolean = false,
            isLoaded: Boolean = false
        ) : this(WeakReference(fragment), isCurrent, isPreset, isLoaded)

        companion object {
            fun getKey(fragment: Fragment): Int = fragment.hashCode()
        }
    }

    private class SharedPoolController(
        fm: FragmentManager,
        private val sharedPool: RecycledViewPool
    ) : FragmentLifecycleCallbacks(), OnPageChangeListener {
        private val actions = SparseArray<AttachAction>()
        private var viewPager: ViewPager? = null
        private var recycledCount = 0
        private var Bundle.isLoaded: Boolean
            get() = getBoolean("isLoaded", false)
            set(value) = putBoolean("isLoaded", value)
        val isBindViewPager: Boolean
            get() = viewPager != null

        init {
            fm.registerFragmentLifecycleCallbacks(this, false)
        }

        fun bindViewPager(viewPager: ViewPager) {
            require(!isBindViewPager) { "已绑定ViewPager。" }
            this.viewPager = viewPager
            viewPager.addOnPageChangeListener(this)
        }

        override fun onFragmentViewCreated(
            fm: FragmentManager,
            fragment: Fragment,
            view: View,
            savedState: Bundle?
        ) {
            val action = getAction(fragment)
            if (action == null) {
                // 当配置更改时，例如旋转屏幕，生命周期回调跟instantiateItem()的调用顺序:
                // onFragmentViewCreated() -> onFragmentResumed() -> instantiateItem()
                // 因此先预设状态，等onFragmentResumed()或者instantiateItem()被调用时，再执行AttachAction。
                presetState(fragment, savedState)
                return
            }
            action.isLoaded = savedState?.isLoaded == true
            if (action.isCurrent || (!action.isCurrent && recycledCount > 0)) {
                action.executeIfLoaded()
            }
        }

        override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
            getAction(fragment)?.execute()
        }

        override fun onFragmentSaveInstanceState(fm: FragmentManager, fragment: Fragment, outState: Bundle) {
            outState.isLoaded = getAction(fragment)?.isLoaded ?: true
        }

        override fun onFragmentViewDestroyed(fm: FragmentManager, fragment: Fragment) {
            getAction(fragment)?.let(::removeAction)
        }

        fun instantiateItem(fragment: Fragment, isCurrent: Boolean) {
            if (fragment.isResumed) {
                return
            }
            val existed = getAction(fragment)
            var isLoaded = false
            if (existed?.isPreset == true) {
                isLoaded = existed.isLoaded
                if (existed.executeIfLoaded()) {
                    return
                }
            }
            val action = AttachAction(
                fragment = fragment,
                isCurrent = isCurrent,
                isLoaded = isLoaded
            )
            actions.put(action.key, action)
        }

        fun destroyItem(fragment: Fragment) {
            if (fragment !is SharedRecycledFragment) {
                return
            }
            fragment.onRecycleToSharedPool(sharedPool)
            ++recycledCount
            actions.executeIfLoaded(
                maxExecute = 1,
                removeIf = { action -> action.fragment === fragment }
            )
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state != ViewPager.SCROLL_STATE_IDLE) {
                return
            }
            // Fragment.onViewCreated()时，若recycledCount = 0，
            // 则会等到destroyItem()被调用时，才执行AttachAction，
            // 但此时可能没有item被销毁，例如滚动到左右边缘再返回的情况，
            // 等到Fragment.onResumed()时才执行AttachAction，恢复体验并不好。
            // 因此当ViewPager滚动停止时，执行全部还未被处理的已加载任务，
            // 发送消息的目的是确保在destroyItem()之后执行。
            viewPager?.post { actions.executeIfLoaded() }
        }

        private fun presetState(fragment: Fragment, savedState: Bundle?) {
            savedState ?: return
            val action = AttachAction(
                fragment = fragment,
                isPreset = true,
                isLoaded = savedState.isLoaded
            )
            actions.put(action.key, action)
        }

        private fun getAction(fragment: Fragment): AttachAction? {
            return actions.get(AttachAction.getKey(fragment))
        }

        private fun removeAction(action: AttachAction) {
            actions.remove(action.key)
        }

        private inline fun SparseArray<AttachAction>.executeIfLoaded(
            maxExecute: Int = size(),
            removeIf: (action: AttachAction) -> Boolean = { false }
        ) {
            var max = if (maxExecute > 0) min(maxExecute, size()) else return
            for (index in size() - 1 downTo 0) {
                val action = valueAt(index)
                if (action.fragment == null || removeIf(action)) {
                    removeAction(action)
                    continue
                }
                if (action.executeIfLoaded()) {
                    if (--max == 0) return
                }
            }
        }

        private fun AttachAction.executeIfLoaded(): Boolean {
            return if (isLoaded) execute() else false
        }

        private fun AttachAction.execute(): Boolean {
            val fragment = fragment ?: return false
            if (!fragment.isHidden
                    && fragment.view != null
                    && fragment is SharedRecycledFragment) {
                recycledCount = max(0, --recycledCount)
                removeAction(this)
                fragment.onAttachSharedPool(sharedPool)
                fragment.initObserve()
                return true
            }
            return false
        }

        override fun onPageSelected(position: Int): Unit = Unit

        override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int): Unit = Unit
    }
}