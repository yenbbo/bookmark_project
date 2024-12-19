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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject

data class MyPost(
    val content: String = "",
    val page: String = "",
    val imageUrl: String? = null,
    val date: String = "",
)

class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

class PostAdapter(val data: MutableList<MyPost>) : RecyclerView.Adapter<PostViewHolder>(){
    override fun getItemCount(): Int = data.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder =
        PostViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostViewHolder, position: Int){
        val post = data[position]
        holder.binding.itemText.text = post.content
        holder.binding.itemPage.text = post.page
        holder.binding.itemDate.text = post.date

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
            db.collectionGroup("posts") // 모든 책에서 posts 컬렉션을 조회
                .whereEqualTo("uid", user.uid) // 현재 사용자가 작성한 글만 필터링
                .orderBy("timestamp", Query.Direction.DESCENDING) // 최신 글 먼저
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