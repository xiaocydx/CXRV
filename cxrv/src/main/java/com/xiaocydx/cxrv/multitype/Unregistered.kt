package com.xiaocydx.cxrv.multitype

/**
 * 未完成多类型注册的标识
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> unregistered(): MutableMultiType<T> {
    return Unregistered as MutableMultiType<T>
}

private object Unregistered : MutableMultiType<Any>() {
    override val size: Int
        get() = error()

    override fun keyAt(viewType: Int): Type<out Any> = error()

    override fun valueAt(index: Int): Type<out Any> = error()

    override fun itemAt(item: Any): Type<out Any> = error()

    override fun register(type: Type<out Any>): Nothing = error()

    private fun error(): Nothing {
        throw IllegalStateException("未完成多类型注册")
    }
}