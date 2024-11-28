package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.book_project.databinding.ActivityMyBookshelfBinding

class MyBookshelfActivity : AppCompatActivity() {
    val binding: ActivityMyBookshelfBinding by lazy {
        ActivityMyBookshelfBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val spinner = binding.spinner
        val sortOptions = listOf("최신순", "별점순")
        val adapter = ArrayAdapter(this, R.layout.custom_spinner, sortOptions)
        adapter.setDropDownViewResource(R.layout.custom_spinner_items)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> sortRecent()
                    1 -> sortRating()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.beforeIcon.setOnClickListener {
            Log.d("MyBookshelfActivity", "뒤로가기 버튼 클릭")
            finish()
        }
    }

    private fun sortRecent() {
        // 최신순 정렬 알고리즘
        Toast.makeText(this, "최신순으로 정렬합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun sortRating() {
        // 별점순 정렬 알고리즘
        Toast.makeText(this, "별점순으로 정렬합니다.", Toast.LENGTH_SHORT).show()
    }
}