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
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.example.book_project.model.Book
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentBookPageBinding

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


}