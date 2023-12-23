package com.xiaocydx.sample.common

/**
 * @author xcc
 * @date 2022/2/17
 */
data class Foo(
    val id: String,
    val name: String,
    val num: Int,
    val url: String = "",
    val type: FooType = FooType.TYPE1
)

enum class FooType {
    TYPE1, TYPE2
}