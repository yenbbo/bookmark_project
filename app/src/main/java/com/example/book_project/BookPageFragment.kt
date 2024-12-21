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
import androidx.compose.foundation.text.selection.Direction
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.example.book_project.model.Book
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentBookPageBinding
import retrofit2.http.Query

class BookPageFragment : Fragment() {

    private lateinit var binding: FragmentBookPageBinding
    private lateinit var book: Book

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

        binding.writingBtn.setOnClickListener {
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
                .orderBy("likes", Query.Direction.DESCENDING) // 좋아요 기준 정렬
                .limit(3) // 상위 3개
                .get()
                .addOnSuccessListener { documents ->
                    val previewContainer = binding.previewCommentsContainer
                    previewContainer.removeAllViews() // 초기화

                    for (doc in documents) {
                        val comment = doc.toObject(Comment::class.java)
                        val commentView = createCommentView(comment)
                        previewContainer.addView(commentView)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookPageFragment", "Error loading top comments", e)
                }
        }
    }

    private fun createCommentView(comment: Comment): View {
        val commentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.comment_preview_item, null)

        val userNameView = commentView.findViewById<TextView>(R.id.previewUserName)
        val ratingBar = commentView.findViewById<RatingBar>(R.id.previewRatingBar)
        val commentTextView = commentView.findViewById<TextView>(R.id.previewComment)
        val likesView = commentView.findViewById<TextView>(R.id.previewLikes)

        userNameView.text = comment.userName
        ratingBar.rating = comment.rating
        commentTextView.text = comment.content
        likesView.text = "좋아요 ${comment.likes}"

        return commentView
    }
}