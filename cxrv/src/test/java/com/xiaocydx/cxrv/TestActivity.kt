package com.xiaocydx.cxrv

import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

/**
 * @author xcc
 * @date 2021/10/12
 */
class TestActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recyclerView = RecyclerView(this).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(recyclerView)
    }
}