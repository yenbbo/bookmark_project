package com.example.book_project

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
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

        // UI 업데이트
        binding.bookTitle.text = book.title
        binding.bookAuthor.text = book.author
        binding.bookDescription.text = book.description
        binding.bookPages.text = "${book.pages} pages"
        binding.bookYear.text = book.year.toString()
        binding.bookPublisher.text = book.publisher
        binding.bookRating.rating = book.rating

        Glide.with(this)
            .load(book.coverUrl)
            .into(binding.bookCover)

        return binding.root
    }

}