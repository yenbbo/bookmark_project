package com.example.book_project

import android.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.appcompat.widget.SearchView


data class Comment(
    val content: String = "",
    val page: String = "",
    val pageNumber: Int = 0,
    val imageUrl: String? = null,
    val isSpoiler: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val uid: String? = "",
    val bookID: String? = ""
) {


    fun getDate(): Date = timestamp.toDate()
    // 날짜 형식 변환
    fun getFormattedDate(): String {
        val date = getDate()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(date)
    }
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
        binding.itemDate.text = comment.getFormattedDate()

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

    private var bookID: String? = null
    private var bookTitle: String? = null

    private val writingActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadComments()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 책 데이터 전달 받기
        bookID = intent.getStringExtra("bookID")
        bookTitle = intent.getStringExtra("bookTitle")
        Log.d("CommentActivity", "bookID: $bookID, bookTitle: $bookTitle")

        setRecyclerView()
        loadComments()
        setupSearchView()
        setupSpinner()

        binding.beforeIcon.setOnClickListener {
            finish()
        }

        binding.writingFab.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            intent.putExtra("bookID", bookID) // 책 데이터 전달
            intent.putExtra("bookTitle", bookTitle)
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

    private fun setupSpinner() {
        val options = arrayOf("최신순", "페이지순", "좋아요순")
        val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadComments(orderBy = "timestamp", direction = Query.Direction.DESCENDING) // 최신순
                    1 -> loadComments(orderBy = "page", direction = Query.Direction.ASCENDING) // 페이지순
                    2 -> {
                        // 좋아요순
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearchView() {
        binding.pageSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrEmpty() || query.toIntOrNull() == null) {
                    Toast.makeText(this@CommentActivity, "숫자를 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    searchCommentsByPage(query.toInt())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 검색창 내용이 비워졌을 때 초기 상태로 복원
                if (newText.isNullOrEmpty()) {
                    loadComments() // 초기 상태로 복원
                }
                return true
            }
        })
        binding.pageSearchView.setOnCloseListener {
            loadComments() // 검색창 닫으면 댓글 목록 초기화
            true
        }
    }

    private fun loadComments(orderBy: String = "timestamp", direction: Query.Direction = Query.Direction.DESCENDING) {
        bookID?.let {
            db.collection("books")
                .document(it)
                .collection("posts")
                .whereEqualTo("bookID", bookID)
                .orderBy(orderBy, direction)
                .get()
                .addOnSuccessListener { documents ->
                    val tempComments = mutableListOf<Comment>()

                    for (doc in documents) {
                        val comment = doc.toObject(Comment::class.java)

                        // `page` 필드를 숫자로 변환
                        val pageNumber = comment.page.removePrefix("p.").toIntOrNull() ?: Int.MAX_VALUE
                        tempComments.add(comment.copy(pageNumber = pageNumber)) // 새로 변환된 필드를 포함
                    }

                    val sortedComments = when (orderBy) {
                        "timestamp" -> tempComments.sortedByDescending { it.timestamp }
                        "page" -> tempComments.sortedBy { it.pageNumber } // 숫자 정렬
                        else -> tempComments
                    }

                    comments.clear()
                    comments.addAll(sortedComments)
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CommentActivity", "Error loading comments", e)
                }
        }
    }

    private fun searchCommentsByPage(page: Int) {
        bookID?.let {
            db.collection("books")
                .document(it)
                .collection("posts")
                .whereEqualTo("page", "p.$page") // 페이지 값은 "p."로 시작
                .get()
                .addOnSuccessListener { documents ->
                    comments.clear()
                    if (documents.isEmpty) {
                        Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        for (doc in documents) {
                            val comment = doc.toObject(Comment::class.java)
                            comments.add(comment)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CommentActivity", "Error searching comments", e)
                }
        }
    }

}