package com.example.book_project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.databinding.ActivityCommentBinding
import com.example.book_project.databinding.ItemPostBinding

data class Comment(val content: String, val page: String, val imageUrl: String? = null, val isSpoiler: Boolean = false)

class CommentViewHolder(val binding: ItemPostBinding): RecyclerView.ViewHolder(binding.root)

class CommentAdapter(val datas: MutableList<Comment>?): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    override fun getItemCount(): Int = datas?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        CommentViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding=(holder as CommentViewHolder).binding
        val comment = datas!![position]
        Log.d("CommentAdapter", "Binding comment: content=${comment.content}, page=${comment.page}, isSpoiler=${comment.isSpoiler}, imageUrl=${comment.imageUrl}")

        // 스포일러가 포함된 글인 경우
        if (comment.isSpoiler) {
            binding.itemText.text = "스포일러가 포함된 글입니다"
            binding.showContent.visibility = View.VISIBLE // "글 보기" 표시
            binding.showContent.setOnClickListener {
                binding.itemText.text = comment.content // 원본 글 표시
                binding.showContent.visibility = View.GONE // "글 보기" 숨김
            }
            binding.itemImage.visibility = View.GONE // 이미지 숨김
        } else { // 스포일러가 포함되지 않은 글인 경우
            binding.itemText.text = comment.content
            binding.showContent.visibility = View.GONE

        }

        binding.itemPage.text = comment.page

        if(!comment.imageUrl.isNullOrEmpty()) {
            binding.itemImage.visibility = View.VISIBLE
            comment.imageUrl.let {
                val imageUri = Uri.parse(it)
                Glide.with(binding.itemImage.context)
                    .load(imageUri)
                    .centerCrop()
                    .into(binding.itemImage)
            }
        }
        else {
            binding.itemImage.visibility = View.GONE
        }

    }

    fun addComment(comment: Comment) {
        datas?.add(comment)
        notifyItemInserted(datas!!.size - 1)
    }
}


class CommentActivity : AppCompatActivity() {
    val binding: ActivityCommentBinding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        binding.commentRecyclerview.layoutManager = layoutManager

        adapter = CommentAdapter(comments)
        binding.commentRecyclerview.adapter = adapter
        binding.commentRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        binding.writingFab.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            startActivityForResult(intent, 1000)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            val content = data?.getStringExtra("content") ?: ""
            val page = data?.getStringExtra("page") ?: ""
            val imageUrlString = data?.getStringExtra("image")
            val isSpoiler = data?.getBooleanExtra("isSpoiler", false)
            Log.d("CommentActivity", "Received content: $content, page: $page, isSpoiler: $isSpoiler, imageUrl: $imageUrlString")

            val imageUrl = imageUrlString?.let {
                Uri.parse(it)
            }

            val comment = Comment(content, page, imageUrlString, isSpoiler!!)
            adapter.addComment(comment)
        }
    }
}