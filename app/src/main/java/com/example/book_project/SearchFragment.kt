package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.book_project.model.Book
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class SearchFragment : Fragment() {
    private lateinit var bookAdapter: BookAdapter // 어댑터에서 아이템 클릭 이벤트 정의
    private lateinit var editTextSearch: EditText
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var navController: NavController
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        val bookItem = view.findViewById<Button>(R.id.book_item)
        bookItem.setOnClickListener {
            navigateToBookPage()
        }

//        리사이클러뷰 설정
        recyclerViewResults = view.findViewById(R.id.recyclerViewResults)
        recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())

//        ui초기화
        editTextSearch = view.findViewById(R.id.editTextSearch)

        bookAdapter = BookAdapter(mutableListOf()) { book ->
            navigateToBookDetailsPage(book) // 아이템 클릭 시 실행할 메서드 호출
        }
        recyclerViewResults.adapter = bookAdapter

        // 검색창에서 검색 버튼 클릭 시
        editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editTextSearch.text.toString()
                if (query.isNotEmpty()) {
                    searchBooks(query)
                }
                true
            } else {
                false
            }
        }
    }


    private fun navigateToBookPage() {
        navController.navigate(R.id.action_searchFragment_to_bookPageFragment)
    }

    private fun navigateToBookDetailsPage(book: Book) {
        // 책 세부 페이지로 이동
        val action = SearchFragmentDirections.actionSearchFragmentToBookPageFragment(book)
        navController.navigate(action)

    }


    private fun searchBooks(query: String) {
        val clientId = "mKoOX4w44sReCYsNmeSY" // 네이버 개발자 센터 Client ID
        val clientSecret = "qzw2KD2QTZ" // 네이버 개발자 센터 Client Secret
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://openapi.naver.com/v1/search/book.json?query=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SearchFragment", "API 호출 실패", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val bookList = mutableListOf<Book>()

                response.body?.let { responseBody ->
                    val json = JSONObject(responseBody.string())
                    val items = json.getJSONArray("items")
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        bookList.add(
                            Book(
                                item.getString("title"),
                                item.getString("author"),
                                item.getString("image"),
                                item.optString("description", ""),
                                item.optDouble("rating", 0.0).toFloat(),
                                item.optInt("pages", 0),
                                item.optInt("year", 0),
                                item.optString("publisher", "")
                            )
                        )
                    }
                }

                requireActivity().runOnUiThread {
                    if (bookList.isNotEmpty()) {
                        bookAdapter.updateBooks(bookList)
                    }
                }
            }
        })
    }
}
