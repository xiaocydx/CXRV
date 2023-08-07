package com.xiaocydx.sample.paging.local

/**
 * @author xcc
 * @date 2022/2/17
 */
enum class MenuAction(val text: String) {
    LINEAR_LAYOUT("LinearLayout"),
    GIRD_LAYOUT("GridLayout"),
    STAGGERED_GRID_LAYOUT("StaggeredGridLayout"),
    INCREASE_SPAN_COUNT("增加SpanCount"),
    DECREASE_SPAN_COUNT("减少SpanCount"),
    REVERSE_LAYOUT("反转Layout"),
    REFRESH("刷新"),
    ADAPTER_INSERT_ITEM("Adapter插入Item"),
    ADAPTER_REMOVE_ITEM("Adapter移除Item"),
    LIST_STATE_INSERT_ITEM("ListState插入Item"),
    LIST_STATE_REMOVE_ITEM("ListState移除Item"),
    CLEAR_ODD_ITEM("清除奇数Item"),
    CLEAR_EVEN_ITEM("清除偶数Item"),
    CLEAR_ALL_ITEM("清除全部Item")
}