package com.xiaocydx.cxrv.layout.compat

import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * item是否为固定尺寸
 *
 * 当需要item动画(add/remove/move动画)，并且频繁做Change更新时，
 * 若item为固定尺寸，则返回true，表示启用Change更新的优化方案。
 *
 * **注意**：可以根据`holder`的数据返回对应的值。
 */
typealias ItemHasFixedSize = (holder: ViewHolder) -> Boolean