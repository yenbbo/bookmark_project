package com.example.book_project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

data class Comment(
    val content: String = "",
    val page: String = "",
    val imageUrl: String? = null,
    val isSpoiler: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val id: String? = ""
) {
    fun getDate(): Date = timestamp.toDate()
}

class CommentViewHolder(val binding: ItemPostBinding): RecyclerView.ViewHolder(binding.root)

class CommentAdapter(val datas: MutableList<Comment>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    override fun getItemCount(): Int = datas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        CommentViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding=(holder as CommentViewHolder).binding
        val comment = datas[position]
        Log.d("CommentAdapter", "Binding comment: $comment")

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
        binding.itemDate.text = comment.getDate().toString()

        if(!comment.imageUrl.isNullOrEmpty()) {
            binding.itemImage.visibility = View.VISIBLE
            Glide.with(binding.itemImage.context)
                .load(Uri.parse(comment.imageUrl))
                .centerCrop()
                .into(binding.itemImage)
        }
        else {
            binding.itemImage.visibility = View.GONE
        }

    }

    fun addComment(comment: Comment) {
        datas.add(0, comment)
        notifyItemInserted(0)
    }

    fun updateComment(comments: List<Comment>) {
        datas.clear()
        datas.addAll(comments)
        notifyDataSetChanged()
    }
}


class CommentActivity : AppCompatActivity() {
    val binding: ActivityCommentBinding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<Comment>()
    private val db = FirebaseFirestore.getInstance()

    private val writingActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadComments()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setRecyclerView()
        loadComments()

        binding.beforeIcon.setOnClickListener {
            finish()
        }

        binding.writingFab.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            writingActivityResultLauncher.launch(intent)
        }
    }

    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.commentRecyclerview.layoutManager = layoutManager
        adapter = CommentAdapter(comments)
        binding.commentRecyclerview.adapter = adapter
        binding.commentRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    private fun loadComments() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                comments.clear()
                for (doc in documents) {
                    val comment = doc.toObject(Comment::class.java)
                    comments.add(comment)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("CommentActivity", "Error loading comments", e)
            }
    }

}