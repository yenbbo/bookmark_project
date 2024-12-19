package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.book_project.databinding.FragmentBookSearchBinding
import com.example.book_project.model.Book
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import okhttp3.Request
import java.io.IOException

class SearchFragment : Fragment() {
    private lateinit var bookAdapter: BookAdapter // 어댑터에서 아이템 클릭 이벤트 정의
    private lateinit var binding: FragmentBookSearchBinding
    private val books = mutableListOf<Book>()

    private val clientId = "mKoOX4w44sReCYsNmeSY"
    private val clientSecret = "qzw2KD2QTZ"
    private val apiUrl = "https://openapi.naver.com/v1/search/book.json"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookSearchBinding.inflate(inflater, container, false)


        // RecyclerView 설정
        bookAdapter = BookAdapter(books) { book ->
            navigateToBookPage(book)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = bookAdapter

        // 검색 버튼 이벤트
        binding.buttonSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                searchBooks(query)
            }
        }

        return binding.root
    }


    private fun searchBooks(query: String) {
        val url = "$apiUrl?query=${query}&display=10"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "API 호출 실패", Toast.LENGTH_SHORT).show()
                }
                Log.e("SearchFragment", "API 호출 실패", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val json = JSONObject(responseBody.string())
                        val items = json.getJSONArray("items")
                        val newBooks = mutableListOf<Book>()

                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val book = Book(
                                title = item.getString("title").replace("<b>", "").replace("</b>", ""),
                                author = item.optString("author", "Unknown"),
                                coverUrl = item.optString("image", ""),
                                description = item.optString("description", ""),
                                rating = 0.0f, // 네이버 API는 평점을 제공 x
                                pages = 0,     // 네이버 API는 페이지 정보를 제공 x
                                year = item.optString("pubdate", "0000").take(4).toIntOrNull() ?: 0,
                                publisher = item.optString("publisher", "Unknown")
                            )
                            newBooks.add(book)
                        }

                        requireActivity().runOnUiThread {
                            if (newBooks.isEmpty()) {
                                Toast.makeText(requireContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                books.clear()
                                books.addAll(newBooks) // 기존 리스트를 새 데이터로 갱신
                                bookAdapter.notifyDataSetChanged() // 어댑터에 새 데이터 전달
                            }
                        }
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "검색 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // BookPageFragment로 이동
    private fun navigateToBookPage(book: Book) {
        val fragment = BookPageFragment()
        val args = Bundle().apply {
            putParcelable("book", book)
        }
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}