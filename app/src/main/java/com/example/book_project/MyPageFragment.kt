package com.example.book_project

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentHomeBinding
import com.example.book_project.databinding.FragmentMyPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageFragment : Fragment() {
    private lateinit var binding: FragmentMyPageBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMyPageBinding.inflate(inflater, container, false)

        val user = auth.currentUser

        user?.let {
            // Firestore에서 사용자 정보 가져오기
            val userRef = db.collection("users").document(it.uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("userName")
                        val userProfileImage = document.getString("userProfileImage")

                        // 사용자 이름 설정
                        binding.userName.text = userName ?: "No Name"

                        // 프로필 사진 설정 (이미지 URL이 있으면 Glide로 로딩)
                        Glide.with(this)
                            .load(userProfileImage)
                            .circleCrop()
                            .placeholder(R.drawable.profile_icon) // 기본 프로필 이미지 설정
                            .into(binding.profileImage)
                    }
                }
                .addOnFailureListener { e ->
                    // Firestore 읽기 실패 처리
                    binding.userName.text = "Error loading data"
                    Glide.with(this)
                        .load(R.drawable.profile_icon) // 기본 프로필 이미지 설정
                        .circleCrop()
                        .into(binding.profileImage)
                }
        }

        binding.logout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.editProfile.setOnClickListener {
            val editProfileFragment = EditProfileFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.myBookshelf.setOnClickListener {
            val intent = Intent(requireContext(), MyBookshelfActivity::class.java)
            startActivity(intent)
        }
        binding.myPost.setOnClickListener {
            val intent = Intent(requireContext(), MyPostsActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }


}