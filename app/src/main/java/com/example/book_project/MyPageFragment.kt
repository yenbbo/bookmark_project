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

class MyPageFragment : Fragment() {
    private lateinit var binding: FragmentMyPageBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMyPageBinding.inflate(inflater, container, false)

        val user = auth.currentUser

        user?.let {
            // 사용자 이름 설정
            binding.userName.text = it.displayName

            // 프로필 사진 설정
            Glide.with(this)
                .load(it.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_icon) // 기본 프로필 이미지 설정
                .into(binding.profileImage)
        }

        binding.editProfile.setOnClickListener {
            // 프로필 수정 화면으로 이동하는 코드
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