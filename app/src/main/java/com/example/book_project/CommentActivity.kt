package com.example.book_project

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.databinding.ActivityCommentBinding
import com.example.book_project.databinding.ItemPostBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.widget.SearchView

data class Comment(
    val content: String = "",
    val page: String = "",
    val imageUrl: String? = null,
    @JvmField var isSpoiler: Boolean = false, // firebase에서 못 찾아서 @JvmField 추가함
    val timestamp: Timestamp = Timestamp.now(),
    val uid: String = "",
    val bookID: String = "",
    var postID: String = "",
    @JvmField var likeCount: Int = 0,
    @JvmField var isLiked: Boolean = false,
    @JvmField var commentCount: Int = 0,
) : Parcelable {

    fun getPageNumber(): Int {
        return page.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
    }

    // Parcelable 생성자
    constructor(parcel: Parcel) : this(
        content = parcel.readString() ?: "",
        page = parcel.readString() ?: "",
        imageUrl = parcel.readString(),
        isSpoiler = parcel.readByte() != 0.toByte(),
        timestamp = parcel.readParcelable(Timestamp::class.java.classLoader) ?: Timestamp.now(),
        uid = parcel.readString() ?: "",
        bookID = parcel.readString() ?: "",
        postID = parcel.readString() ?: "",
        likeCount = parcel.readInt(),
        isLiked = parcel.readByte() != 0.toByte(),
        commentCount = parcel.readInt()
    )

    // Parcelable의 데이터를 Parcel에 저장하는 메서드
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(content)
        parcel.writeString(page)
        parcel.writeString(imageUrl)
        parcel.writeByte(if (isSpoiler) 1 else 0)
        parcel.writeParcelable(timestamp, flags)
        parcel.writeString(uid)
        parcel.writeString(bookID)
        parcel.writeString(postID)
        parcel.writeInt(likeCount)
        parcel.writeByte(if (isLiked) 1 else 0)
        parcel.writeInt(commentCount)
    }

    // Parcelable의 다른 메서드
    override fun describeContents(): Int = 0

    // Parcelable을 생성하기 위한 CREATOR 객체
    companion object CREATOR : Parcelable.Creator<Comment> {
        override fun createFromParcel(parcel: Parcel): Comment {
            return Comment(parcel)
        }

        override fun newArray(size: Int): Array<Comment?> {
            return arrayOfNulls(size)
        }
    }

    // 날짜 형식 변환
    fun getDate(): Date = timestamp.toDate()

    fun getFormattedDate(): String {
        val date = getDate()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(date)
    }

}

class CommentViewHolder(val binding: ItemPostBinding): RecyclerView.ViewHolder(binding.root)

class CommentAdapter(val datas: MutableList<Comment>, private val onItemClick: (Comment) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val db = FirebaseFirestore.getInstance()
    override fun getItemCount(): Int { return datas.size }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        CommentViewHolder(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding=(holder as CommentViewHolder).binding
        val comment = datas[position]
        val currentUID = FirebaseAuth.getInstance().currentUser?.uid
        val userName = FirebaseAuth.getInstance().currentUser?.displayName

        Log.d("CommentAdapter", "Binding comment at position: $position, data: $comment")

        if(!comment.imageUrl.isNullOrEmpty()) {
            binding.itemImage.visibility = View.VISIBLE
            Glide.with(binding.itemImage.context)
                .load(Uri.parse(comment.imageUrl))
                .centerCrop()
                .into(binding.itemImage)
        }
        else {
            binding.itemImage.visibility = View.GONE
        }
        // 스포일러가 포함된 글인 경우
        if (comment.isSpoiler) {
            binding.itemText.text = "스포일러가 포함된 글입니다"
            binding.showContent.visibility = View.VISIBLE // "글 보기" 표시
            binding.showContent.setOnClickListener {
                binding.itemText.text = comment.content // 원본 글 표시
                binding.showContent.visibility = View.GONE // "글 보기" 숨김
                comment.isSpoiler = false // 스포일러 여부 변경
                notifyItemChanged(position) // 변경된 내용 갱신
            }
        }
        else { // 스포일러가 포함되지 않은 글인 경우
            binding.itemText.text = comment.content
            binding.showContent.visibility = View.GONE
        }


        // 좋아요 버튼 클릭 처리
        binding.likeButton.setOnClickListener {
            if (currentUID != null){
                val likesRef = db.collection("books")
                    .document(comment.bookID)
                    .collection("posts")
                    .document(comment.postID)
                    .collection("likes")
                    .document(currentUID)
                likesRef.get().addOnSuccessListener { document ->
                    val isLiked = document.exists()
                    val newLikeCount = if (isLiked) comment.likeCount - 1 else comment.likeCount + 1

                    // Firestore에서 좋아요 상태 변경
                    if (isLiked) {
                        // 좋아요 취소
                        likesRef.delete()
                    } else {
                        // 좋아요 추가
                        likesRef.set(hashMapOf("liked" to true))
                    }

                    // 게시물의 좋아요 수 업데이트
                    db.collection("books")
                        .document(comment.bookID)
                        .collection("posts")
                        .document(comment.postID)
                        .update("likeCount", newLikeCount)
                        .addOnSuccessListener {
                            comment.likeCount = newLikeCount
                            binding.likeCount.text = newLikeCount.toString()

                            // UI 업데이트
                            if (isLiked) {
                                binding.likeButton.setImageResource(R.drawable.favor_icon)
                            } else {
                                binding.likeButton.setImageResource(R.drawable.filled_favor_icon)
                            }
                            // 좋아요 알림 전송
                            if (userName != null) {
                                sendLikeNotification(comment.uid, userName, comment.postID, comment.bookID)
                            }
                        }
                }
            }
        }

        binding.itemDate.text = comment.getFormattedDate()
        binding.itemPage.text = comment.page

        // 글 클릭 이벤트 설정
        binding.root.setOnClickListener {
            Log.d("CommentAdapter", "Clicked comment at position: $position, data: $comment")
            onItemClick(comment) // 클릭된 댓글 데이터 전달
        }

        // 좋아요 상태에 맞는 버튼 이미지 설정
        binding.likeCount.text = comment.likeCount.toString()
        if (currentUID != null) {
            val likesRef = db.collection("books")
                .document(comment.bookID)
                .collection("posts")
                .document(comment.postID)
                .collection("likes")
                .document(currentUID)

            likesRef.get().addOnSuccessListener { document ->
                // 유저가 좋아요를 눌렀는지 확인
                if (document.exists()) {
                    binding.likeButton.setImageResource(R.drawable.filled_favor_icon)
                } else {
                    binding.likeButton.setImageResource(R.drawable.favor_icon)
                }
            }
        }
    }

    private fun sendLikeNotification(receiverUID: String, userName: String, postID: String, bookID: String) {
        val notification = hashMapOf(
            "type" to "like",
            "senderUID" to FirebaseAuth.getInstance().currentUser?.uid,
            "receiverUID" to receiverUID,
            "postID" to postID,
            "bookID" to bookID,
            "message" to "$userName 님이 내 코멘트에 좋아요를 남겼습니다.",
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )

        db.collection("notifications")
            .document(receiverUID)
            .collection("userNotifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Notification sent to $receiverUID")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Failed to send notification", e)
            }
    }

}


class CommentActivity : AppCompatActivity() {
    val binding: ActivityCommentBinding by lazy {
        ActivityCommentBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<Comment>()
    private val db = FirebaseFirestore.getInstance()

    private var bookID: String? = null
    private var bookTitle: String? = null

    private val writingActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadComments()
        }
    }

    private val commentDetailActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isUpdated = result.data?.getBooleanExtra("isUpdated", false) ?: false
                if (isUpdated) {
                    loadComments() // 데이터 갱신
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupSpinner()
        setupSearchView()

        // 책 데이터 전달 받기
        bookID = intent.getStringExtra("bookID")
        bookTitle = intent.getStringExtra("bookTitle")

        setRecyclerView()
        loadComments()

        binding.beforeIcon.setOnClickListener {
            finish()
        }

        binding.writingFab.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            intent.putExtra("bookID", bookID) // 책 데이터 전달
            intent.putExtra("bookTitle", bookTitle)
            writingActivityResultLauncher.launch(intent)
        }

    }

    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.commentRecyclerview.layoutManager = layoutManager
        adapter = CommentAdapter(comments) { comment ->
            navigateToCommentDetail(comment)
        }
        binding.commentRecyclerview.adapter = adapter
        binding.commentRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    private fun setupSpinner() {
        val options = arrayOf("최신순", "페이지순", "좋아요순")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadComments(orderBy = "timestamp", direction = Query.Direction.DESCENDING) // 최신순
                    1 -> loadComments(orderBy = "page", direction = Query.Direction.ASCENDING) // 페이지순
                    2 -> loadComments(orderBy = "likeCount", direction = Query.Direction.DESCENDING) // 좋아요순
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearchView() {
        binding.pageSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrEmpty() || query.toIntOrNull() == null) {
                    Toast.makeText(this@CommentActivity, "숫자를 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    searchCommentsByPage(query.toInt().toString())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadComments() // 초기 상태로 복원
                }
                return true
            }
        })

        binding.pageSearchView.setOnCloseListener {
            loadComments() // 검색창 닫으면 초기화
            true
        }
    }

    // 페이지 검색 기능
    @SuppressLint("NotifyDataSetChanged")
    private fun searchCommentsByPage(page: String) {
        bookID?.let {

            val formattedPage = "p.$page" // "p." 형식 추가

            db.collection("books")
                .document(it)
                .collection("posts")
                .whereEqualTo("page", formattedPage) // 문자열 기준 검색
                .get()
                .addOnSuccessListener { documents ->
                    comments.clear()

                    // 검색 결과가 비어있는지 확인
                    if (documents.isEmpty) {
                        Toast.makeText(
                            this@CommentActivity,
                            "해당 페이지에 댓글이 없습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        for (doc in documents) {
                            val comment = doc.toObject(Comment::class.java)
                            comments.add(comment)
                        }
                    }

                    // 결과 반영
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CommentActivity", "Error searching comments", e)
                    Toast.makeText(
                        this@CommentActivity,
                        "댓글 검색 중 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun loadComments(
        orderBy: String = "timestamp",
        direction: Query.Direction = Query.Direction.DESCENDING
    ) {
        bookID?.let {
            db.collection("books")
                .document(bookID!!)
                .collection("posts")
                .get()
                .addOnSuccessListener { documents ->
                    comments.clear()

                    val tempList = mutableListOf<Comment>()
                    for (doc in documents) {
                        val comment = doc.toObject(Comment::class.java)
                        if (doc.id.isNotEmpty()) {
                            comment.postID = doc.id
                        }
                        tempList.add(comment)
                    }

                    when (orderBy) {
                        "likeCount" -> {
                            tempList.sortWith(
                                compareByDescending<Comment> { it.likeCount }
                                    .thenBy { it.timestamp }
                            )
                        }
                        "page" -> { // 페이지 순 정렬
                            tempList.sortWith(
                                compareBy<Comment> { it.getPageNumber() }
                                    .thenBy { it.timestamp }
                            )
                        }
                        "timestamp" -> {
                            tempList.sortByDescending { it.timestamp }
                        }
                    }

                    comments.addAll(tempList)
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CommentActivity", "Error loading comments", e)
                }
        }
    }



    private fun navigateToCommentDetail(comment: Comment) {
        val intent = Intent(this, CommentDetailActivity::class.java).apply {
            putExtra("postID", comment.postID)
            putExtra("bookID", comment.bookID)
            putExtra("commentData", comment) // 댓글 데이터 전체 전달
        }
        Log.d("CommentActivity", "Navigating to CommentDetailActivity with data: ${comment.postID}, ${comment.bookID}")
        commentDetailActivityResultLauncher.launch(intent)
    }

}