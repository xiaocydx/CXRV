package com.xiaocydx.sample.paging

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
    ADAPTER_DELETE_ITEM("Adapter删除Item"),
    LIST_STATE_INSERT_ITEM("ListState插入Item"),
    LIST_STATE_DELETE_ITEM("ListState删除Item"),
    CLEAR_ODD_ITEM("清除奇数项"),
    CLEAR_EVEN_ITEM("清除偶数项"),
    CLEAR_ALL_ITEM("清除全部Item")
}