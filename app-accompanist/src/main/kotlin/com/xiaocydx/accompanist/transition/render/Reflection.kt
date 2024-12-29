package com.xiaocydx.accompanist.transition.render

import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author xcc
 * @date 2024/12/29
 */
internal interface Reflection {

    val Class<*>.declaredStaticFields: List<Field>
        get() {
            val fields = runCatching {
                if (Build.VERSION.SDK_INT < 28) {
                    declaredFields.filter { Modifier.isStatic(it.modifiers) }
                } else {
                    HiddenApiBypass.getStaticFields(this)
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (fields.getOrNull() ?: emptyList<Field>()) as List<Field>
        }

    val Class<*>.declaredInstanceFields: List<Field>
        get() {
            val fields = runCatching {
                if (Build.VERSION.SDK_INT < 28) {
                    declaredFields.filter { !Modifier.isStatic(it.modifiers) }
                } else {
                    HiddenApiBypass.getInstanceFields(this)
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (fields.getOrNull() ?: emptyList<Field>()) as List<Field>
        }

    fun Class<*>.declaredConstructor(vararg parameterTypes: Class<*>): Constructor<*> {
        return if (Build.VERSION.SDK_INT < 28) {
            getDeclaredConstructor(*parameterTypes)
        } else {
            HiddenApiBypass.getDeclaredConstructor(this, *parameterTypes)
        }
    }

    fun Class<*>.declaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
        return if (Build.VERSION.SDK_INT < 28) {
            getDeclaredMethod(name, *parameterTypes)
        } else {
            HiddenApiBypass.getDeclaredMethod(this, name, *parameterTypes)
        }
    }

    fun Constructor<*>.toCache() = ConstructorCache(this)

    fun Method.toCache() = MethodCache(this)

    fun Field.toCache() = FieldCache(this)

    fun List<Field>.find(name: String): Field = first { it.name == name }

    fun List<Field>.findOrNull(name: String): Field? = find { it.name == name }
}

@JvmInline
internal value class ConstructorCache(private val constructor: Constructor<*>) {
    init {
        constructor.isAccessible = true
    }

    @Suppress("SpellCheckingInspection")
    fun newInstance(vararg initargs: Any): Any? {
        return runCatching { constructor.newInstance(*initargs) }.getOrNull()
    }
}

@JvmInline
internal value class FieldCache(private val field: Field) {
    init {
        field.isAccessible = true
    }

    fun get(obj: Any?): Any? {
        return runCatching { field.get(obj) }.getOrNull()
    }

    fun set(obj: Any?, value: Any?): Boolean {
        return runCatching { field.set(obj, value) }.isSuccess
    }
}

@JvmInline
internal value class MethodCache(private val method: Method) {
    init {
        method.isAccessible = true
    }

    fun invoke(obj: Any?, vararg args: Any?) {
        kotlin.runCatching { method.invoke(obj, *args) }
    }
}