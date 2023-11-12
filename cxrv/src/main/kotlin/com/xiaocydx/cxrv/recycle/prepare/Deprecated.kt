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

@file:JvmName("PrepareScrapDeprecatedKt")
@file:Suppress("PackageDirectoryMismatch", "DEPRECATION")

package androidx.recyclerview.widget

import android.annotation.SuppressLint
import android.util.SparseIntArray
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.*
import com.xiaocydx.cxrv.recycle.prepare.PrepareFlow
import com.xiaocydx.cxrv.recycle.prepare.dispatcher
import com.xiaocydx.cxrv.recycle.prepare.frameTimeDeadline
import com.xiaocydx.cxrv.recycle.prepare.prepareHolder
import com.xiaocydx.cxrv.recycle.prepare.putToRecycledViewPool
import com.xiaocydx.cxrv.recycle.prepare.reuse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 按[PrepareScope.add]的添加顺序，预创建ViewHolder并放入[RecycledViewPool]
 *
 * **注意**：该函数需要在视图初始化阶段调用，如果有自定义[RecycledViewPool]，
 * 那么在调用该函数之前，先对RecyclerView设置自定义[RecycledViewPool]，例如：
 * ```
 * lifecycleScope.launch {
 *     recyclerView.setRecycledViewPool(CustomRecycledViewPool())
 *     val result = recyclerView.prepareScrap(
 *         prepareAdapter = adapter,
 *         prepareDeadline = PrepareDeadline.FOREVER_NS,
 *         prepareDispatcher = Dispatchers.IO
 *     ) {
 *         add(viewType1, 20)
 *         add(viewType2, 10)
 *     }
 *     result.getRecycledScrapCount()
 *     result.getPreparedScrapCount()
 * }
 * ```
 *
 * @param prepareAdapter 对RecyclerView设置的Adapter可能是[ConcatAdapter]，
 * 因此需要主动传入跟列表数据关联的Adapter，用于观察数据变更和创建ViewHolder，
 * 若调用[Adapter.createViewHolder]抛出异常，则结束预创建流程，并传递异常。
 *
 * @param prepareDeadline 预创建的截止时间，默认为[PrepareDeadline.FOREVER_NS]，
 * 预创建流程会按[prepareDeadline]进行停止，尽可能避免创建多余的ViewHolder。
 *
 * @param prepareDispatcher 用于预创建的协程调度器，默认为[Dispatchers.IO]，
 * 可以通过`Dispatchers.IO.limitedParallelism()`创建一个并行度受限的调度器，
 * 供多处调用该函数的地方使用。
 *
 * @param block 调用[PrepareScope.add]，添加预创建的键值对，添加顺序决定创建顺序，
 * 如果对RecyclerView设置的[RecycledViewPool]，已经存在ViewHolder（共享池场景），
 * 那么可以先减去[RecycledViewPool.getRecycledViewCount]，再添加预创建的键值对。
 *
 * @return [PrepareResult]提供数量查询函数，可用于数据统计或者单元测试。
 */
@Deprecated(
    message = "预创建流程不安全，配置属性有些冗余",
    replaceWith = ReplaceWith(
        expression = "PrepareScrap",
        imports = ["com.xiaocydx.cxrv.recycle.prepare.PrepareScrap"]
    )
)
@MainThread
@SuppressLint("CheckResult")
suspend fun RecyclerView.prepareScrap(
    prepareAdapter: Adapter<*>,
    prepareDeadline: PrepareDeadline = PrepareDeadline.FOREVER_NS,
    prepareDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: PrepareScope.() -> Unit
): PrepareResult {
    assertMainThread()
    val rv = this
    val pairs = PrepareScope().apply(block).getPairs()
    val initialCapacity = pairs.sumOf { it.count }
    val result = PrepareResult(initialCapacity, recycledViewPool)
    if (initialCapacity <= 0) return result

    val prepareScrap = rv.prepareHolder().dispatcher(prepareDispatcher)
    if (prepareDeadline == PrepareDeadline.FRAME_NS) {
        prepareScrap.frameTimeDeadline(prepareAdapter)
    }
    var prepareFlow: PrepareFlow<ViewHolder>? = null
    pairs.forEach { (viewType, count) ->
        prepareFlow = if (prepareFlow == null) {
            prepareScrap.reuse(viewType, count, prepareAdapter)
        } else {
            prepareFlow!!.reuse(viewType, count, prepareAdapter)
        }
    }
    prepareFlow?.putToRecycledViewPool()?.collect(result::putScrapToRecycledViewPool)
    return result
}

/**
 * 预创建的截止时间
 */
@Deprecated(
    message = "预创建流程不安全，配置属性有些冗余",
    replaceWith = ReplaceWith(
        expression = "PrepareScrap",
        imports = ["com.xiaocydx.cxrv.recycle.prepare.PrepareScrap"]
    )
)
enum class PrepareDeadline {
    /**
     * 没有截止时间
     */
    FOREVER_NS,

    /**
     * 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
     */
    FRAME_NS
}

/**
 * 调用[PrepareScope.add]，添加预创建的键值对
 */
@Deprecated(
    message = "预创建流程不安全，配置属性有些冗余",
    replaceWith = ReplaceWith(
        expression = "PrepareScrap",
        imports = ["com.xiaocydx.cxrv.recycle.prepare.PrepareScrap"]
    )
)
class PrepareScope internal constructor() {
    private val pairs = mutableListOf<Pair>()

    fun add(viewType: Int, @IntRange(from = 1) count: Int) {
        if (count < 1) return
        pairs.add(Pair(viewType, count))
    }

    internal fun getPairs(): List<Pair> = ArrayList(pairs)

    /**
     * 不使用[kotlin.Pair]，有装箱拆箱的开销
     */
    internal data class Pair(val viewType: Int, val count: Int)
}

/**
 * [RecyclerView.prepareScrap]的结果，提供数量查询函数
 */
@Deprecated(
    message = "预创建流程不安全，配置属性有些冗余",
    replaceWith = ReplaceWith(
        expression = "PrepareScrap",
        imports = ["com.xiaocydx.cxrv.recycle.prepare.PrepareScrap"]
    )
)
@MainThread
class PrepareResult internal constructor(
    initialCapacity: Int,
    private val recycledViewPool: RecycledViewPool
) {
    private val preparedScrapCount = if (initialCapacity > 0) SparseIntArray() else null

    /**
     * 获取[viewType]在[RecycledViewPool]的回收数量
     *
     * 回收数量不一定等于[getPreparedScrapCount]，可能布局流程已取走一部分进行填充，
     * 又或者在执行预创建流程之前，[RecyclerView]已存在ViewHolder，例如共享池场景。
     */
    fun getRecycledScrapCount(viewType: Int): Int {
        assertMainThread()
        return recycledViewPool.getRecycledViewCount(viewType)
    }

    /**
     * 获取[viewType]在[RecycledViewPool]的预创建数量
     */
    fun getPreparedScrapCount(viewType: Int): Int {
        assertMainThread()
        return preparedScrapCount?.get(viewType) ?: 0
    }

    internal fun putScrapToRecycledViewPool(scrap: Scrap<ViewHolder>) {
        assertMainThread()
        preparedScrapCount?.apply {
            val count = get(scrap.viewType)
            put(scrap.viewType, count + 1)
        }
    }
}