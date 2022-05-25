package com.xiaocydx.cxrv.multitype

/**
 * ViewType的映射类型
 *
 * @author xcc
 * @date 2021/10/8
 */
data class Type<T : Any>
@PublishedApi internal constructor(
    val clazz: Class<T>,
    val delegate: ViewTypeDelegate<T, *>
)