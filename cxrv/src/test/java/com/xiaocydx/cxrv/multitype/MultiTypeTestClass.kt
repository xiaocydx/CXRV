package com.xiaocydx.cxrv.multitype

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

internal class TestItem
internal class TypeTestItem(
    val type: TestType
)

internal enum class TestType {
    TYPE_A, TYPE_B
}

internal fun<T: Any> mutableMultiTypeOf() = MutableMultiTypeImpl<T>()

internal open class TestDelegate : ViewTypeDelegate<TestItem, ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        error("onCreateViewHolder")
    }

    override fun onBindViewHolder(holder: ViewHolder, item: TestItem) {
    }

    override fun areItemsTheSame(oldItem: TestItem, newItem: TestItem): Boolean = true
}

internal open class TypeADelegate : ViewTypeDelegate<TypeTestItem, ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        error("onCreateViewHolder")
    }

    override fun onBindViewHolder(holder: ViewHolder, item: TypeTestItem) {
    }

    override fun areItemsTheSame(oldItem: TypeTestItem, newItem: TypeTestItem): Boolean {
        return oldItem.type == newItem.type
    }
}

internal open class TypeBDelegate : ViewTypeDelegate<TypeTestItem, ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        error("onCreateViewHolder")
    }

    override fun onBindViewHolder(holder: ViewHolder, item: TypeTestItem) {
    }

    override fun areItemsTheSame(oldItem: TypeTestItem, newItem: TypeTestItem): Boolean {
        return oldItem.type == newItem.type
    }
}