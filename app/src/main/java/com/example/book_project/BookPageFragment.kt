package com.example.book_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.book_project.model.Book
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentBookPageBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class BookPageFragment : Fragment() {

    private lateinit var binding: FragmentBookPageBinding
    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            book = it.getParcelable("book")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookPageBinding.inflate(inflater, container, false)

        binding.beforeIcon.setOnClickListener {
            parentFragmentManager.popBackStack() // 검색창으로 돌아가기
        }

        binding.bookYearDetail.text = book.year.toString()
        binding.bookPublisherDetail.text = book.publisher
        binding.writingBtnMain.setOnClickListener {
            val intent = Intent(requireContext(), WritingActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            Log.d("BookPageFrag", "bookID: ${book.id}, bookTitle: ${book.title}")
            startActivity(intent)
        }

        binding.buttonViewAll.setOnClickListener {
            val intent = Intent(requireContext(), CommentActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            Log.d("BookPageFrag", "bookID: ${book.id}, bookTitle: ${book.title}")
            startActivity(intent)
        }

        binding.bookDescription.post {
            if (binding.bookDescription.lineCount > 6) {
                binding.ExpandDescription.visibility = View.VISIBLE
            }
        }

        // "더보기" 버튼 클릭 이벤트
        binding.ExpandDescription.setOnClickListener {
            binding.bookDescription.maxLines = Int.MAX_VALUE
            binding.ExpandDescription.visibility = View.GONE
        }

        // UI 업데이트
        binding.bookTitle.text = book.title
        binding.bookAuthor.text = book.author
        binding.bookDescription.text = book.description
        binding.bookYear.text = book.year.toString()
        binding.bookPublisher.text = book.publisher
        binding.bookRating.rating = book.rating

        Glide.with(this)
            .load(book.coverUrl)
            .into(binding.bookCover)

        return binding.root
    }

    private fun loadTopComments() {
        book.id?.let { bookID ->
            db.collection("books")
                .document(bookID)
                .collection("posts")
                .orderBy("likeCount", Query.Direction.DESCENDING) // 좋아요 기준 정렬
                .orderBy("timestamp", Query.Direction.ASCENDING) // 좋아요 수가 같을 경우 시간 순으로 정렬
                .limit(3) // 상위 3개
                .get()
                .addOnSuccessListener { documents ->
                    val previewContainer = binding.previewCommentsContainer
                    previewContainer.removeAllViews() // 기존 뷰 초기화

                    for (doc in documents) {
                        val comment = doc.toObject(Comment::class.java)

                        val userDocRef = db.collection("users").document(comment.uid)
                        userDocRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                val userName = document.getString("userName") ?: "Unknown" // 닉네임 불러오기

                                val commentView = createCommentView(comment, userName)
                                previewContainer.addView(commentView) // 미리보기 추가
                            }
                        }.addOnFailureListener { e ->
                            Log.e("loadTopComments", "Failed to load user data", e)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookPageFragment", "Error loading top comments", e)
                }
        }
    }

    private fun createCommentView(comment: Comment, userName: String): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_comment, null)

        val commentContent = view.findViewById<TextView>(R.id.commentText)
        val commentUserName = view.findViewById<TextView>(R.id.commentName) // 닉네임 추가
        val commentLikeCount = view.findViewById<TextView>(R.id.like_count)

        commentContent.text = comment.content
        commentUserName.text = userName // 닉네임 적용
        commentLikeCount.text = comment.likeCount.toString()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 상단 UI 초기화 및 데이터 로드
        binding.beforeIcon.setOnClickListener {
            parentFragmentManager.popBackStack() // 검색창으로 돌아가기
        }

        binding.writingBtn.setOnClickListener {
            val intent = Intent(requireContext(), WritingActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            startActivity(intent)
        }

        binding.buttonViewAll.setOnClickListener {
            val intent = Intent(requireContext(), CommentActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            startActivity(intent)
        }

        // 상단 설명 더보기 처리
        binding.bookDescription.post {
            if (binding.bookDescription.lineCount > 6) {
                binding.ExpandDescription.visibility = View.VISIBLE
            }
        }

        binding.ExpandDescription.setOnClickListener {
            binding.bookDescription.maxLines = Int.MAX_VALUE
            binding.ExpandDescription.visibility = View.GONE
        }

        // 데이터 로드
        loadTopComments()
    }



}