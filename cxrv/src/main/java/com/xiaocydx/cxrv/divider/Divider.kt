package com.xiaocydx.cxrv.divider

import android.content.Context
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.divider.DividerItemDecoration

/**
 * 添加通用间隔，可用于链式调用场景
 *
 * @param width  水平方向的间隔size
 * @param height 垂直方向的间隔size
 */
fun <T : RecyclerView> T.spacing(
    @Px width: Int = 0,
    @Px height: Int = 0
): T = divider {
    this.width = width
    this.height = height
}

/**
 * 添加通用分割线，可用于链式调用场景
 *
 * ```
 * recyclerView.divider {
 *     width = 5.dp
 *     height = 5.dp
 *     color = 0xFF9DAA8F.toInt()
 *     verticalEdge = true
 *     horizontalEdge = true
 * }
 * ```
 * 详细的属性描述[DividerItemDecoration.Config]
 */
inline fun <T : RecyclerView> T.divider(
    block: DividerItemDecoration.Config.() -> Unit
): T {
    addItemDecoration(DividerItemDecoration(context, block))
    return this
}

/**
 * 创建通用分割线
 *
 * ```
 * DividerItemDecoration(context) {
 *     width = 5.dp
 *     height = 5.dp
 *     color = 0xFF9DAA8F.toInt()
 *     verticalEdge = true
 *     horizontalEdge = true
 * }
 * ```
 * 详细的属性描述[DividerItemDecoration.Config]
 */
inline fun DividerItemDecoration(
    context: Context,
    block: DividerItemDecoration.Config.() -> Unit
): DividerItemDecoration = DividerItemDecoration.Config(context).apply(block).build()