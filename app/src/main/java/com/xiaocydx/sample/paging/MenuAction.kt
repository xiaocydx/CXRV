package com.xiaocydx.sample.paging

/**
 * @author xcc
 * @date 2022/2/17
 */
enum class MenuAction(val text: String) {
    LINEAR_LAYOUT("LinearLayout"),
    GIRD_LAYOUT("GridLayout"),
    STAGGERED_LAYOUT("StaggeredLayout"),
    INCREASE_SPAN_COUNT("增加SpanCount"),
    DECREASE_SPAN_COUNT("减少SpanCount"),
    REVERSE_LAYOUT("反转Layout"),
    REFRESH("刷新"),
    ADAPTER_INSERT_ITEM("Adapter插入Item"),
    ADAPTER_DELETE_ITEM("Adapter删除Item"),
    PAGER_INSERT_ITEM("Pager插入Item"),
    PAGER_DELETE_ITEM("Pager删除Item"),
    CLEAR_ODD_ITEM("清除奇数项"),
    CLEAR_EVEN_ITEM("清除偶数项"),
    CLEAR_ALL_ITEM("清除全部Item"),
    FORWARD_ITEM_EVENT("跳转ItemEvent页面")
}