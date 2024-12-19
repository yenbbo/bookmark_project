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

        binding.logout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.editProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
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