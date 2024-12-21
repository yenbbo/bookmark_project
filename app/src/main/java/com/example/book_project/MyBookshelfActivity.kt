package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.databinding.ActivityMyBookshelfBinding
import com.example.book_project.databinding.ItemBookshelfBinding
import com.example.book_project.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class BookViewHolder(val binding: ItemBookshelfBinding) : RecyclerView.ViewHolder(binding.root)

class BookShelfAdapter(
    private val bookShelfList: List<Book>
) : RecyclerView.Adapter<BookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookshelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookShelfList[position]
        val binding = holder.binding

        // 책 커버 이미지 로드
        Glide.with(binding.root.context)
            .load(book.coverUrl)
            .into(binding.bookCover)

        // 별점 표시 (예: 0.0~5.0 사이의 값)
        binding.bookRating.text = book.rating.toString()
    }

    override fun getItemCount(): Int = bookShelfList.size
}

class MyBookshelfActivity : AppCompatActivity() {
    private val binding: ActivityMyBookshelfBinding by lazy {
        ActivityMyBookshelfBinding.inflate(layoutInflater)
    }

    private var ratedBooks: List<Book> = mutableListOf()  // 책 목록을 저장할 변수
    private var initialBookList: List<Book> = mutableListOf()

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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.beforeIcon.setOnClickListener {
            Log.d("MyBookshelfActivity", "뒤로가기 버튼 클릭")
            finish()
        }

        // RecyclerView에 GridLayoutManager 설정
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Firebase에서 데이터 로드 후 RecyclerView에 설정
        loadRatedBooks()
    }

    private fun loadRatedBooks() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("books")
            .whereGreaterThan("ratings.$userID", 0)  // 사용자가 평가한 책만 가져오기
            .get()
            .addOnSuccessListener { documents ->
                ratedBooks = documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java).apply {
                        id = doc.id
                        rating = (doc.get("ratings.$userID") as? Double)?.toFloat() ?: 0f
                    }
                }
                initialBookList = ratedBooks  // 처음 로드된 리스트를 저장
                updateBookshelfUI(ratedBooks)
            }
            .addOnFailureListener { e ->
                Log.e("MyBookshelfActivity", "책 데이터 로드 실패", e)
            }
    }

    private fun updateBookshelfUI(books: List<Book>) {
        // 책 목록을 RecyclerView의 어댑터에 설정
        binding.recyclerView.adapter = BookShelfAdapter(books)
    }

    private fun sortRecent() {
        // 최신순 정렬 알고리즘
        ratedBooks = initialBookList
        updateBookshelfUI(ratedBooks)
        Toast.makeText(this, "최신순으로 정렬합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun sortRating() {
        // 별점순 정렬 알고리즘
        ratedBooks = ratedBooks.sortedByDescending { it.rating }
        updateBookshelfUI(ratedBooks)
        Toast.makeText(this, "별점순으로 정렬합니다.", Toast.LENGTH_SHORT).show()
    }
}
