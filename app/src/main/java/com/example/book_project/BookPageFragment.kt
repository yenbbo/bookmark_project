package com.example.book_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.example.book_project.model.Book
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentBookPageBinding

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

        // 뒤로 가기 버튼
        binding.beforeIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 책 기본 정보 설정
        binding.bookTitle.text = book.title
        binding.bookAuthor.text = book.author
        binding.bookDescription.text = book.description
        binding.bookYear.text = book.year.toString()
        binding.bookPublisher.text = book.publisher
        binding.bookRating.rating = book.rating

        // 책 표지 이미지 설정
        Glide.with(this)
            .load(book.coverUrl)
            .into(binding.bookCover)

        // 별점 저장
        binding.bookRating.setOnRatingBarChangeListener { _, rating, _ ->
            saveRatingToFirestore(book.id, rating)
        }

        // 글쓰기 버튼 클릭
        binding.writingBtn.setOnClickListener {
            val intent = Intent(requireContext(), WritingActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            startActivity(intent)
        }

        // 전체 댓글 보기 버튼
        binding.buttonViewAll.setOnClickListener {
            val intent = Intent(requireContext(), CommentActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            startActivity(intent)
        }

        // 설명 더보기 설정
        binding.bookDescription.post {
            if (binding.bookDescription.lineCount > 6) {
                binding.ExpandDescription.visibility = View.VISIBLE
            }
        }
        binding.ExpandDescription.setOnClickListener {
            binding.bookDescription.maxLines = Int.MAX_VALUE
            binding.ExpandDescription.visibility = View.GONE
        }

        loadTopComments() // 상위 댓글 로드

        return binding.root
    }

    // Firestore에 별점 저장
    private fun saveRatingToFirestore(bookID: String, rating: Float) {
        val db = FirebaseFirestore.getInstance()
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val bookRef = db.collection("books").document(bookID)

        bookRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                bookRef.update("ratings.$userID", rating)
                    .addOnSuccessListener {
                        Log.d("BookPageFragment", "별점 저장 완료.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookPageFragment", "별점 저장 실패", e)
                    }
            } else {
                val newBookData = mapOf(
                    "title" to book.title,
                    "author" to book.author,
                    "publisher" to book.publisher,
                    "year" to book.year,
                    "coverUrl" to book.coverUrl,
                    "ratings" to mapOf(userID to rating)
                )

                bookRef.set(newBookData)
                    .addOnSuccessListener {
                        Log.d("BookPageFragment", "책 데이터 저장 완료.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookPageFragment", "책 데이터 저장 실패", e)
                    }
            }
        }
    }

    // 상위 댓글 불러오기
    private fun loadTopComments() {
        db.collection("books")
            .document(book.id)
            .collection("posts")
            .orderBy("likeCount", Query.Direction.DESCENDING) // 좋아요 기준 정렬
            .orderBy("timestamp", Query.Direction.ASCENDING) // 좋아요 수가 같을 경우 시간 순으로 정렬
            .limit(3) // 상위 3개
            .get()
            .addOnSuccessListener { documents ->
                val previewContainer = binding.previewCommentsContainer
                previewContainer.removeAllViews()

                for (doc in documents) {
                    val comment = doc.toObject(Comment::class.java)

                    // 닉네임 가져오기
                    val userDocRef = db.collection("users").document(comment.uid)
                    userDocRef.get().addOnSuccessListener { document ->
                        val userName = document.getString("userName") ?: "Unknown"
                        val commentView = createCommentView(comment, userName)
                        previewContainer.addView(commentView)
                    }.addOnFailureListener { e ->
                        Log.e("loadTopComments", "닉네임 불러오기 실패", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookPageFragment", "댓글 불러오기 실패", e)
            }
    }

    // 댓글 뷰 생성
    private fun createCommentView(comment: Comment, userName: String): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_comment, null)

        val commentContent = view.findViewById<TextView>(R.id.commentText)
        val commentUserName = view.findViewById<TextView>(R.id.commentName)
        val commentLikeCount = view.findViewById<TextView>(R.id.like_count)

        commentContent.text = comment.content
        commentUserName.text = userName
        commentLikeCount.text = comment.likeCount.toString()

        return view
    }

    // UI 초기화 및 데이터 로드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 글쓰기 버튼 클릭
        binding.writingBtn.setOnClickListener {
            val intent = Intent(requireContext(), WritingActivity::class.java)
            intent.putExtra("bookID", book.id)
            intent.putExtra("bookTitle", book.title)
            startActivity(intent)
        }

        // 댓글 전체 보기 클릭
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

    }
}
