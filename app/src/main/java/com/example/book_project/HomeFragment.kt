package com.example.book_project

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.book_project.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkUnreadNotifications()
        binding.notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkUnreadNotifications() {
        if (currentUserUid == null) return

        // Firestore에서 읽지 않은 알림 확인
        db.collection("users")
            .document(currentUserUid)
            .collection("notifications")
            .whereEqualTo("isRead", false) // 읽지 않은 알림 필터
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // 읽지 않은 알림이 없을 경우 기본 아이콘 표시
                    binding.notificationIcon.setImageResource(R.drawable.noti_icon)
                } else {
                    // 읽지 않은 알림이 있을 경우 강조된 아이콘 표시
                    binding.notificationIcon.setImageResource(R.drawable.noti_unread_icon)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                binding.notificationIcon.setImageResource(R.drawable.noti_icon)
            }
    }

}