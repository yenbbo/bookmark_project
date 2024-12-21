package com.example.book_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.book_project.databinding.ActivityNotificationBinding
import com.example.book_project.databinding.ItemNotificationBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Notification(
    val type: String = "", // "like", "comment", 등
    val senderUID: String = "",
    val receiverUID: String = "",
    val postID: String? = null,
    val bookID: String? = null,
    val commentID: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val message: String = "",
    var isRead: Boolean = false
)
class NotificationViewHolder(val binding: ItemNotificationBinding) :
    RecyclerView.ViewHolder(binding.root)


class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = notifications[position]
        val binding=(holder as NotificationViewHolder).binding

        binding.notificationMessage.text = notification.message

        binding.root.setOnClickListener {
            // 알림을 읽음 상태로 업데이트
            markNotificationAsRead(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    private fun markNotificationAsRead(notification: Notification) {
        val db = FirebaseFirestore.getInstance()
        val currentUID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("notifications")
            .document(currentUID)
            .collection("userNotifications")
            .whereEqualTo("timestamp", notification.timestamp)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    db.collection("notifications")
                        .document(currentUID)
                        .collection("userNotifications")
                        .document(doc.id)
                        .update("isRead", true)
                }
            }
    }
}

class NotificationActivity : AppCompatActivity() {
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private val db = FirebaseFirestore.getInstance()

    val binding: ActivityNotificationBinding by lazy {
        ActivityNotificationBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.beforeIcon.setOnClickListener {
            finish()
        }
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifications)
        binding.notificationRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.notificationRecyclerview.adapter = adapter
        binding.notificationRecyclerview.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadNotifications() {
        val currentUID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("notifications")
            .document(currentUID)
            .collection("userNotifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("NotificationActivity", "Error fetching notifications", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    notifications.clear()
                    for (doc in snapshots.documents) {
                        val notification = doc.toObject(Notification::class.java)
                        notification?.let { notifications.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}