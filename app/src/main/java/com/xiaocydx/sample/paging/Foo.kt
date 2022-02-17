package com.xiaocydx.sample.paging

/**
 * @author xcc
 * @date 2022/2/17
 */
data class Foo(
    val id: String,
    val name: String,
    val num: Int,
    val type: FooType = FooType.TYPE1
)

enum class FooType {
    TYPE1, TYPE2
}