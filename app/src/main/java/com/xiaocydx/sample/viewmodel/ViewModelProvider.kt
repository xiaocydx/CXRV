package com.xiaocydx.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

class ViewModelLazy<VM : ViewModel>(
    private val viewModelKey: String,
    private val viewModelClass: Class<VM>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null
    private var storeProducer: (() -> ViewModelStore)? = storeProducer
    private var factoryProducer: (() -> ViewModelProvider.Factory)? = factoryProducer

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer!!()
                val store = storeProducer!!()
                factoryProducer = null
                storeProducer = null
                ViewModelProvider(store, factory).get(viewModelKey, viewModelClass).also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}