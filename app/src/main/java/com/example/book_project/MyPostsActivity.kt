package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.book_project.databinding.ActivityMyPostsBinding
import com.example.book_project.databinding.ItemNotificationBinding
import com.example.book_project.databinding.ItemPostBinding

class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

class PostAdapter(val data: MutableList<String>) : RecyclerView.Adapter<PostViewHolder>(){
    override fun getItemCount(): Int = data.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder =
        PostViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostViewHolder, position: Int){
        holder.binding.itemText.text = data[position]
    }
}

class MyPostsActivity : AppCompatActivity() {
    val binding: ActivityMyPostsBinding by lazy {
        ActivityMyPostsBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val data = mutableListOf<String>()
        for(i in 1..10){
            data.add("post $i")
        }
        binding.postsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.postsRecyclerview.adapter = PostAdapter(data)
        binding.postsRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        binding.beforeIcon.setOnClickListener {
            Log.i("MyPostsActivity", "finish")
            finish()
        }
    }
}