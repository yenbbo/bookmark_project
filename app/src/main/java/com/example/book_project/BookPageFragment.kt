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

        binding.bookRating.setOnRatingBarChangeListener { _, rating, _ ->
            saveRatingToFirestore(book.id, rating)
        }

        binding.writingBtn.setOnClickListener {
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

    private fun saveRatingToFirestore(bookID: String, rating: Float) {
        val db = FirebaseFirestore.getInstance()
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return // 현재 로그인한 사용자 ID 가져오기
        val timestamp = System.currentTimeMillis()

        // Firestore에서 책의 문서 참조
        val bookRef = db.collection("books").document(bookID)

        // 문서가 존재하는지 확인
        bookRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // 문서가 존재하면 ratings 필드에 사용자별로 별점 업데이트
                bookRef.update("ratings.$userID", rating)
                    .addOnSuccessListener {
                        Log.d("BookPageFragment", "별점이 성공적으로 저장되었습니다.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookPageFragment", "별점 저장 실패", e)
                    }
            } else {
                // 문서가 존재하지 않으면 새로운 문서 생성
                val newBookData = mapOf(
                    "title" to book.title,
                    "author" to book.author,
                    "publisher" to book.publisher,
                    "year" to book.year,
                    "coverUrl" to book.coverUrl,
                    "ratings" to mapOf(userID to rating) // ratings에 userID와 별점 추가
                )

                bookRef.set(newBookData)
                    .addOnSuccessListener {
                        Log.d("BookPageFragment", "책 데이터가 새로 생성되었습니다.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookPageFragment", "책 데이터 생성 실패", e)
                    }
            }
        }
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