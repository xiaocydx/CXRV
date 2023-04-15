/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("ConcatAdapterHolderInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.xiaocydx.cxrv.concat.SpanSizeProvider
import java.lang.reflect.Field

/**
 * [ConcatAdapter]的持有者
 *
 * 由于[getSpanSize]会被频繁调用，而每次访问[ConcatAdapter.getAdapters]都会创建新的集合对象，
 * 因此不能在[getSpanSize]中访问[ConcatAdapter.getAdapters]，效率较低。
 *
 * 若在[ConcatAdapterHolder]初始化时，将[ConcatAdapter.getAdapters]保存为`adapters`，
 * 则`adapters`会存在更新问题，即[ConcatAdapter.addAdapter]或[ConcatAdapter.removeAdapter]被调用时，
 * 由于`adapters`只是副本，它无法被同步更新，继续通过这个`adapters`获取spanSize将会出现异常。
 *
 * ### [ReflectConcatAdapterHolder]
 * 反射获取[ConcatAdapter.mController.mWrappers]解决更新问题。
 *
 * ### [ObserveConcatAdapterHolder]
 * 观察局部刷新解决更新问题，反射失败后的替代方案。
 *
 * @author xcc
 * @date 2021/10/1
 */
@Suppress("KDocUnresolvedReference")
internal interface ConcatAdapterHolder {

    val concatAdapter: ConcatAdapter

    /**
     * 实际场景设置的原始[SpanSizeLookup]
     */
    val sourceLookup: SpanSizeLookup

    /**
     * 获取[concatAdapter]中符合[globalPosition]的adapter，
     * 若adapter实现了[SpanSizeProvider]，则通过[SpanSizeProvider.getSpanSize]获取spanSize。
     *
     * ConcatAdapter(adapter1，adapter2，adapter3)
     *
     * ### GlobalPosition
     * * adapter1：[0, 1]，itemCount = 2
     * * adapter2: [2, 3]，itemCount = 2
     * * adapter3: [4, 5]，itemCount = 2
     *
     * ### LocalPosition
     * * adapter1：[0, 1]
     * * adapter2: [0, 1]，即[2, 3] - adapter1.itemCount
     * * adapter3: [0, 1]，即[4, 5] - adapter1.itemCount - adapter2.itemCount
     */
    fun getSpanSize(globalPosition: Int, spanCount: Int): Int {
        var localPosition = globalPosition
        for (index in 0 until getAdapterSize()) {
            val adapter = getAdapter(index)
            when {
                localPosition < adapter.itemCount -> return when (adapter) {
                    is SpanSizeProvider -> adapter.getSpanSize(localPosition, spanCount)
                    else -> sourceLookup.getSpanSize(globalPosition)
                }
                else -> localPosition -= adapter.itemCount
            }
        }
        return sourceLookup.getSpanSize(globalPosition)
    }

    fun getAdapterSize(): Int

    fun getAdapter(index: Int): Adapter<*>

    fun dispose()
}

internal fun ConcatAdapterHolder(
    concatAdapter: ConcatAdapter,
    sourceLookup: SpanSizeLookup
): ConcatAdapterHolder = ReflectConcatAdapterHolder(concatAdapter, sourceLookup)
    .takeIf { it.reflectSucceed } ?: ObserveConcatAdapterHolder(concatAdapter, sourceLookup)

private class ReflectConcatAdapterHolder(
    override val concatAdapter: ConcatAdapter,
    override val sourceLookup: SpanSizeLookup
) : ConcatAdapterHolder {
    /**
     * 反射获取[ConcatAdapter.mController.mWrappers]，仅用于遍历访问adapter元素，不做修改操作。
     * 当[ConcatAdapter.addAdapter]或[ConcatAdapter.removeAdapter]被调用时，`mWrappers`会被同步更新。
     */
    @Suppress("KDocUnresolvedReference")
    private val wrappers: List<NestedAdapterWrapper>
    val reflectSucceed: Boolean

    init {
        val wrappers = concatAdapter.getWrappers()
        this.wrappers = wrappers ?: emptyList()
        reflectSucceed = wrappers != null
    }

    override fun getAdapterSize() = wrappers.size

    override fun getAdapter(index: Int): Adapter<*> = wrappers[index].adapter

    override fun dispose() = Unit

    private companion object {
        /**
         * [ConcatAdapter.mController]
         */
        val mControllerField: Field? = try {
            ConcatAdapter::class.java
                .getDeclaredField("mController")
                .apply { isAccessible = true }
        } catch (e: NoSuchFieldException) {
            null
        }

        /**
         * [ConcatAdapterController.mWrappers]
         */
        val mWrappersField: Field? = try {
            ConcatAdapterController::class.java
                .getDeclaredField("mWrappers")
                .apply { isAccessible = true }
        } catch (e: NoSuchFieldException) {
            null
        }

        /**
         * [ConcatAdapter.mController.mWrappers]
         */
        @Suppress("UNCHECKED_CAST", "KDocUnresolvedReference")
        fun ConcatAdapter.getWrappers(): List<NestedAdapterWrapper>? {
            val controller = mControllerField?.get(this) as? ConcatAdapterController ?: return null
            return mWrappersField?.get(controller) as? List<NestedAdapterWrapper>
        }
    }
}

private class ObserveConcatAdapterHolder(
    override val concatAdapter: ConcatAdapter,
    override val sourceLookup: SpanSizeLookup
) : AdapterDataObserver(), ConcatAdapterHolder {
    /**
     * 调用[ConcatAdapter.addAdapter]，添加成功后会触发[onItemRangeInserted]，
     * 调用[ConcatAdapter.removeAdapter]，移除成功后会触发[onItemRangeRemoved]，
     * 因此在[onItemRangeInserted]和[onItemRangeRemoved]中更新[adapters]。
     *
     * **注意**：普通的局部刷新场景也可能触发[onItemRangeInserted]和[onItemRangeRemoved]，
     * 因此这种方式不算是一个高效的方案。
     */
    private var adapters: List<Adapter<*>> = concatAdapter.adapters

    init {
        concatAdapter.registerAdapterDataObserver(this)
    }

    override fun getAdapterSize() = adapters.size

    override fun getAdapter(index: Int): Adapter<*> = adapters[index]

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        adapters = concatAdapter.adapters
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        adapters = concatAdapter.adapters
    }

    override fun dispose() {
        concatAdapter.unregisterAdapterDataObserver(this)
    }
}