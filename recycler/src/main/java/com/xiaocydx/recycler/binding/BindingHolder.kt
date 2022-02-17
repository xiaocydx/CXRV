package com.xiaocydx.recycler.binding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding

typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

class BindingHolder<VB : ViewBinding>(val binding: VB) : ViewHolder(binding.root)