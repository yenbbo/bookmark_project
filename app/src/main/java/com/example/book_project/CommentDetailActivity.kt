package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.book_project.databinding.ActivityCommentDetailBinding
import com.example.book_project.databinding.ItemCommentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Reply(
    val content: String = "",
    val page: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val uid: String = "",
    val bookID: String = "",
    val postID: String = "",
    var replyID: String = "",
    @JvmField var likeCount: Int = 0,
    @JvmField var isLiked: Boolean = false,
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(timestamp.toDate())
    }
}

class CommentDetailViewHolder(val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root)

class CommentDetailAdapter(val datas: MutableList<Reply>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentDetailViewHolder {
        return CommentDetailViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val reply = datas[position]
        val binding=(holder as CommentDetailViewHolder).binding
        val currentUID = FirebaseAuth.getInstance().currentUser?.uid

        // 사용자 이름과 프로필 이미지를 Firestore에서 가져오기
        val userDocRef = db.collection("users").document(reply.uid)
        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userName = document.getString("userName") ?: "Unknown"
                val profileImageUrl = document.getString("userProfileImage")

                binding.commentName.text = userName
                Glide.with(binding.commentProfile.context)
                    .load(profileImageUrl ?: R.drawable.profile_icon) // Firebase에서 가져온 프로필 이미지 URL
                    .circleCrop()
                    .into(binding.commentProfile)
            }
        }

        // 댓글 내용 설정
        binding.commentText.text = reply.content

        // 좋아요 버튼 클릭 처리
        binding.likeButton.setOnClickListener {
            if (currentUID != null) {
                val likesRef = db.collection("books")
                    .document(reply.bookID)
                    .collection("posts")
                    .document(reply.postID)
                    .collection("likes")
                    .document(currentUID)

                likesRef.get().addOnSuccessListener { document ->
                    val isLiked = document.exists()
                    val newLikeCount = if (isLiked) reply.likeCount - 1 else reply.likeCount + 1

                    // Firestore에서 좋아요 상태 변경
                    if (isLiked) {
                        likesRef.delete()
                    } else {
                        likesRef.set(hashMapOf("liked" to true))
                    }

                    // 댓글의 좋아요 수 업데이트
                    db.collection("books")
                        .document(reply.bookID)
                        .collection("posts")
                        .document(reply.postID)
                        .update("likeCount", newLikeCount)
                        .addOnSuccessListener {
                            reply.likeCount = newLikeCount
                            binding.likeCount.text = newLikeCount.toString()

                            // UI 업데이트
                            if (isLiked) {
                                binding.likeButton.setImageResource(R.drawable.favor_icon)
                            } else {
                                binding.likeButton.setImageResource(R.drawable.filled_favor_icon)
                            }
                            // 좋아요 후 알림 보내기
                            val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown User"
                            sendLikeNotification(reply.uid, userName, reply.postID, reply.bookID)
                        }
                }
            }
        }

        // 좋아요 수 업데이트
        binding.likeCount.text = reply.likeCount.toString()

        // 작성 날짜 표시
        binding.commentDate.text = reply.getFormattedDate()

        // 삭제 버튼은 현재는 보이지 않지만, 필요한 경우 표시하도록 설정
        if (currentUID == reply.uid) {  // 현재 사용자와 댓글 작성자가 같을 경우 삭제 버튼 표시
            binding.deleteButton.visibility = View.VISIBLE
            binding.deleteButton.setOnClickListener {
                // 댓글만 삭제
                deleteComment(reply)
            }
        } else {
            binding.deleteButton.visibility = View.GONE
        }
    }

    private fun sendLikeNotification(receiverUID: String, userName: String, postID: String, bookID: String) {
        val notification = Notification(
            type = "like",
            senderUID = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            receiverUID = receiverUID,
            postID = postID,
            bookID = bookID,
            message = "$userName 님이 내 댓글에 좋아요를 남겼습니다.",
            timestamp = Timestamp.now(),
            isRead = false
        )

        db.collection("notifications")
            .document(receiverUID)
            .collection("userNotifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Like notification sent to $receiverUID")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error sending like notification", e)
            }
    }

    private fun deleteComment(reply: Reply) {
        db.collection("books")
            .document(reply.bookID)
            .collection("posts")
            .document(reply.postID)
            .collection("replies")
            .document(reply.replyID)
            .delete()
            .addOnSuccessListener {
                // 댓글이 삭제된 후 RecyclerView 갱신
                datas.remove(reply)
                notifyDataSetChanged()  // RecyclerView 갱신
                Log.d("CommentDetailActivity", "Reply deleted successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("CommentDetailActivity", "Error deleting reply", e)
            }
    }

}

class CommentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentDetailBinding
    private val db = FirebaseFirestore.getInstance()

    private var postID: String? = null
    private var bookID: String? = null
    private val replies = mutableListOf<Reply>()
    private lateinit var adapter: CommentDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postID = intent.getStringExtra("postID")
        bookID = intent.getStringExtra("bookID")
        val commentData = intent.getParcelableExtra<Comment>("commentData")
        Log.d("CommentDetailActivity", "Received commentData: $commentData")

        if (commentData != null) {
            // Handle commentData and display
        } else {
            Log.e("CommentDetailActivity", "commentData is null")
        }

        setupRecyclerView()
        displayCommentDetails(commentData)
        loadReplies()

        binding.uploadButton.setOnClickListener {
            val replyContent = binding.commentInput.text.toString()
            if (replyContent.isNotEmpty()) {
                postReply(replyContent)
            }
        }
        binding.beforeIcon.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)
        adapter = CommentDetailAdapter(replies)
        binding.commentRecyclerview.adapter = adapter
        binding.commentRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    private fun displayCommentDetails(comment: Comment?) {
        comment?.let {
            binding.itemText.text = it.content
            binding.itemDate.text = it.getFormattedDate()
            binding.likeCount.text = it.likeCount.toString()
            binding.commentCount.text = it.commentCount.toString()
            binding.likeButton.setImageResource(
                if (it.isLiked) R.drawable.filled_favor_icon else R.drawable.favor_icon
            )
            binding.itemImage.visibility = if (it.imageUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
            Glide.with(binding.itemImage.context)
                .load(it.imageUrl)
                .centerCrop()
                .into(binding.itemImage)
            // 글 작성자의 정보 가져오기
            loadUserProfile(it.uid)
            binding.postPage.text = it.page
        }
    }

    private fun loadUserProfile(uid: String) {
        // Firestore에서 사용자 정보 가져오기
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Firestore에서 이름과 프로필 이미지 URL 가져오기
                    val userName = document.getString("userName") ?: "Unknown User"
                    val profileImageUrl = document.getString("userProfileImage")

                    // 사용자 이름 설정
                    binding.postName.text = userName

                    // 프로필 이미지 설정
                    Glide.with(this)
                        .load(profileImageUrl ?: R.drawable.profile_icon) // 기본 프로필 이미지 설정
                        .circleCrop()
                        .into(binding.postProfile)
                } else {
                    Log.e("CommentDetailActivity", "User document does not exist in Firestore.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommentDetailActivity", "Error fetching user info", e)
            }
    }

    private fun loadReplies() {
        if (postID != null && bookID != null) {
            db.collection("books")
                .document(bookID!!)
                .collection("posts")
                .document(postID!!)
                .collection("replies")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("CommentDetailActivity", "Error loading replies", e)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        val newReplies = mutableListOf<Reply>()
                        for (doc in snapshots.documents) {
                            val reply = doc.toObject(Reply::class.java)
                            reply?.let {
                                it.replyID = doc.id
                                newReplies.add(it)
                            }
                        }
                        replies.clear()
                        replies.addAll(newReplies)
                        adapter.notifyDataSetChanged() // RecyclerView 갱신
                    }
                }
        }
    }

    private fun postReply(content: String) {
        if (postID != null && bookID != null) {
            val reply = Reply(
                content = content,
                uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                timestamp = Timestamp.now(),
                bookID = bookID!!,
                postID = postID!!,
                replyID = "",
                likeCount = 0,  // 새 댓글은 처음에 좋아요가 0으로 시작
            )

            db.collection("books")
                .document(bookID!!)
                .collection("posts")
                .document(postID!!)
                .collection("replies")
                .add(reply)
                .addOnSuccessListener {
                    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown User"
                    sendCommentNotification(reply.uid, userName, content, postID!!, bookID!!)
                    // Firestore에서 데이터가 실시간으로 갱신되므로 여기서 리스트를 수동으로 업데이트하지 않음
                    binding.commentInput.text.clear()
                }
                .addOnFailureListener { e ->
                    Log.e("CommentDetailActivity", "Error posting reply", e)
                }
        }
    }
    private fun sendCommentNotification(receiverUID: String, userName: String, replyContent: String, postID: String, bookID: String) {
        val notification = Notification(
            type = "comment",
            senderUID = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            receiverUID = receiverUID,
            postID = postID,
            bookID = bookID,
            message = "$userName 님이 내 코멘트에 댓글을 남겼습니다: $replyContent",
            timestamp = Timestamp.now(),
            isRead = false
        )

        db.collection("notifications")
            .document(receiverUID)
            .collection("userNotifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Comment notification sent to $receiverUID")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error sending comment notification", e)
            }
    }
}