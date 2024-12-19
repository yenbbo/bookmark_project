package com.example.book_project

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.model.Book

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {


    inner class BookViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        private val bookTitle: TextView = itemview.findViewById(R.id.bookTitle)  // TextView 참조
        private val bookAuthor: TextView = itemview.findViewById(R.id.bookAuthor)  // TextView 참조
        private val bookCover: ImageView = itemview.findViewById(R.id.bookCover)  // ImageView 참조

        fun bind(book: Book) {
            bookTitle.text = book.title
            bookAuthor.text = book.author
            Glide.with(itemView).load(book.coverUrl).into(bookCover)

            itemView.setOnClickListener {
                onItemClick(book)  // 클릭 시 navigateToBookPage 호출
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }


}
