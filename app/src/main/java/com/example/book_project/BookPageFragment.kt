package com.example.book_project

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import com.example.book_project.model.Book
import com.bumptech.glide.Glide


class BookPageFragment : Fragment() {

    private lateinit var book: Book

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_book_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // SafeArgs로 전달된 데이터 가져오기
        arguments?.let {
            val args = BookPageFragmentArgs.fromBundle(it)
            book = args.book ?: return@let // Null 안전 처리
        }

        // UI 업데이트

        view.findViewById<TextView>(R.id.bookTitle).text = book.title
        view.findViewById<TextView>(R.id.bookAuthor).text = book.author
        view.findViewById<TextView>(R.id.bookDescription).text = book.description
        view.findViewById<TextView>(R.id.bookPages).text = "${book.pages} pages"
        view.findViewById<TextView>(R.id.bookYear).text = book.year.toString()
        view.findViewById<TextView>(R.id.bookPublisher).text = book.publisher
        view.findViewById<RatingBar>(R.id.bookRating).rating = book.rating

        // Glide 이미지 로드
        Glide.with(this)
            .load(book.coverUrl)
            .into(view.findViewById<ImageView>(R.id.bookCover))

        // 뒤로가기 버튼 처리
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }


}