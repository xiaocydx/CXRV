package com.xiaocydx.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.sample.paging.PagingActivity

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startPagingActivity(view: View) {
        startActivity<PagingActivity>()
    }

    private inline fun <reified T : Activity> startActivity() {
        startActivity(Intent(this, T::class.java))
    }
}