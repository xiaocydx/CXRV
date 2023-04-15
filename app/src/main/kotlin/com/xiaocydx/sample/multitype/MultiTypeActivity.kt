package com.xiaocydx.sample.multitype

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xiaocydx.sample.R
import com.xiaocydx.sample.multitype.onetomany.OneToManyFragment
import com.xiaocydx.sample.multitype.onetoone.OneToOneFragment

/**
 * item多类型示例代码
 *
 * @author xcc
 * @date 2022/2/17
 */
class MultiTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multitype)
        initOneToOneFragment(null)
    }

    fun initOneToOneFragment(view: View?) {
        replaceFragment(OneToOneFragment())
    }

    fun initOneToManyFragment(view: View?) {
        replaceFragment(OneToManyFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.flContainer, fragment)
            .commit()
        supportActionBar?.title = fragment.javaClass.simpleName
    }
}