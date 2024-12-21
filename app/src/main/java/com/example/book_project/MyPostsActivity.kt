package com.example.book_project

import android.net.Uri
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
import com.bumptech.glide.Glide
import com.example.book_project.databinding.ActivityMyPostsBinding
import com.example.book_project.databinding.ItemNotificationBinding
import com.example.book_project.databinding.ItemPostBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MyPost(
    val content: String = "",
    val page: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val bookTitle: String = "",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val commentCount: Int = 0,
) {
    fun getDate(): Date = timestamp.toDate()
    // 날짜 형식 변환
    fun getFormattedDate(): String {
        val date = getDate()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(date)
    }
}

class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

class PostAdapter(val data: MutableList<MyPost>) : RecyclerView.Adapter<PostViewHolder>(){
    override fun getItemCount(): Int = data.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder =
        PostViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostViewHolder, position: Int){
        val post = data[position]
        holder.binding.itemText.text = post.content
        holder.binding.itemPage.text = post.page
        holder.binding.itemDate.text = post.getFormattedDate()
        holder.binding.itemTitle.visibility = ViewGroup.VISIBLE
        holder.binding.likeCount.text = post.likeCount.toString()
        holder.binding.commentCount.text = post.commentCount.toString()

        var bookTitle = post.bookTitle
        val shortTitle = bookTitle.substringBefore("(").trim() // 책 제목에 괄호가 나오면 제거
        holder.binding.itemTitle.text = shortTitle

        if(!post.imageUrl.isNullOrEmpty()){
            holder.binding.itemImage.visibility = ViewGroup.VISIBLE
            Glide.with(holder.binding.itemImage.context)
                .load(post.imageUrl)
                .centerCrop()
                .into(holder.binding.itemImage)
        } else {
            holder.binding.itemImage.visibility = ViewGroup.GONE
        }
    }

    fun updatePosts(newPost: List<MyPost>){
        data.clear()
        data.addAll(newPost)
        notifyDataSetChanged()
    }
}

class MyPostsActivity : AppCompatActivity() {
    val binding: ActivityMyPostsBinding by lazy {
        ActivityMyPostsBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: PostAdapter
    private val myPosts = mutableListOf<MyPost>()
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setRecyclerView()
        loadMyPosts()

        binding.beforeIcon.setOnClickListener {
            finish()
        }
    }

    private fun setRecyclerView(){
        binding.postsRecyclerview.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(myPosts)
        binding.postsRecyclerview.adapter = adapter
        binding.postsRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    private fun loadMyPosts(){
        user?.let { user ->
            db.collectionGroup("posts") // 모든 `posts` 서브컬렉션에 대해 쿼리
                .whereEqualTo("uid", user.uid) // 사용자가 작성한 글만 필터링
                .orderBy("timestamp", Query.Direction.DESCENDING) // 최신순 정렬
                .get()
                .addOnSuccessListener { documents ->
                    val posts = documents.map { doc ->
                        doc.toObject(MyPost::class.java)
                    }
                    adapter.updatePosts(posts)
                }
                .addOnFailureListener { e ->
                    Log.e("MyPostsActivity", "Error loading posts: ", e)
                }
        }
    }
}