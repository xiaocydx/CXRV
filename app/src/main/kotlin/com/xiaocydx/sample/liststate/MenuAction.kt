package com.xiaocydx.sample.liststate

/**
 * @author xcc
 * @date 2023/8/17
 */
enum class MenuAction(val text: String) {
    NORMAL("普通列表"),
    PAGING("分页列表"),
    REFRESH("刷新"),
    LIST_STATE_INSERT_ITEM("ListState插入Item"),
    LIST_STATE_REMOVE_ITEM("ListState移除Item"),
    CLEAR_ODD_ITEM("清除奇数Item"),
    CLEAR_EVEN_ITEM("清除偶数Item"),
    CLEAR_ALL_ITEM("清除全部Item")
}