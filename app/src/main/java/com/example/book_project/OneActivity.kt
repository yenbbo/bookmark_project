package com.example.book_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.book_project.databinding.ActivityOneBinding

class OneActivity : AppCompatActivity() {
    val binding: ActivityOneBinding by lazy {
        ActivityOneBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
        // bottomNavigation 클릭 이벤트
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.menu_home -> HomeFragment()
                R.id.menu_search -> SearchFragment()
                R.id.menu_myPage -> MyPageFragment()
                else -> HomeFragment()
            }
            replaceFragment(selectedFragment)
            true
        }
    }
    //fragment 전환
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


}