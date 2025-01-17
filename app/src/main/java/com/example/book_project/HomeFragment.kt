package com.example.book_project

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentHomeBinding
import com.example.book_project.databinding.ItemBookBinding
import com.example.book_project.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeAdapter(private val bookList: List<Book>) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    // 뷰 홀더
    class HomeViewHolder(val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val book = bookList[position]
        val binding = holder.binding

        // 책 커버 이미지 로드
        Glide.with(binding.root.context)
            .load(book.coverUrl)
            .into(binding.bookCover) // 커버 이미지를 표시할 ImageView

    }

    override fun getItemCount(): Int = bookList.size
}


class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val currentUID = FirebaseAuth.getInstance().currentUser?.uid
    private val bookList: MutableList<Book> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 설정
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        val adapter = HomeAdapter(bookList)
        binding.recyclerView.adapter = adapter

        binding.notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        loadUserBooks()
    }

    private fun loadUserBooks() {
        if (currentUID == null) return

        // 사용자가 별점을 매긴 책 쿼리
        db.collection("books")
            .whereGreaterThanOrEqualTo("ratings.$currentUID", 0) // 별점을 매긴 책을 가져옴
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val coverUrl = doc.getString("coverUrl")
                    val title = doc.getString("title")
                    if (coverUrl != null && title != null) {
                        // Book 객체 생성하여 bookList에 추가
                        val book = Book(
                            coverUrl = coverUrl,
                            title = title
                        )
                        bookList.add(book)
                    }
                }
                binding.recyclerView.adapter?.notifyDataSetChanged() // 데이터가 변경된 후 RecyclerView 갱신
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }

        // 사용자가 글을 작성한 책 쿼리
        db.collectionGroup("posts")
            .whereEqualTo("uid", currentUID)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { post ->
                    val bookID = post.getString("bookID")
                    if (bookID != null) {
                        db.collection("books").document(bookID)
                            .get()
                            .addOnSuccessListener { book ->
                                val coverUrl = book.getString("coverUrl")
                                val title = book.getString("title")
                                if (coverUrl != null && title != null) {
                                    val book = Book(
                                        coverUrl = coverUrl,
                                        title = title
                                    )
                                    if (!bookList.contains(book)) { // 중복된 책은 추가하지 않기
                                        bookList.add(book)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                            }
                    }
                }
                binding.recyclerView.adapter?.notifyDataSetChanged() // 데이터가 변경된 후 RecyclerView 갱신
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }



}