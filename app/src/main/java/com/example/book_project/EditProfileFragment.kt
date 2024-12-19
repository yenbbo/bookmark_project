package com.example.book_project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.book_project.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore


class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        val currentUser = auth.currentUser

        // 초기화
        currentUser?.let {
            binding.editUserName.setText(it.displayName)
            Glide.with(this)
                .load(it.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_icon)
                .into(binding.editProfileImage)
        }

        // 프로필 이미지 변경 버튼 클릭 이벤트
        binding.changeProfileImageButton.setOnClickListener {
            openGallery()
        }

        // 저장 버튼 클릭 이벤트
        binding.saveProfileButton.setOnClickListener {
            saveProfile()
        }

        binding.beforeIcon.setOnClickListener {
            parentFragmentManager.popBackStack() // 뒤로가기
        }

        return binding.root
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == AppCompatActivity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.editProfileImage.setImageURI(selectedImageUri)
        }
    }

    private fun saveProfile() {
        val newUserName = binding.editUserName.text.toString().trim()

        if (newUserName.isEmpty()) {
            Toast.makeText(requireContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        currentUser?.let {
            val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(newUserName)
                .setPhotoUri(selectedImageUri)
                .build()

            it.updateProfile(userProfileChangeRequest)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "프로필 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

            // Firestore에 사용자 정보 업데이트
            val userRef = db.collection("users").document(it.uid)
            val updatedData = hashMapOf<String, Any>(
                "name" to newUserName,
                "photoUrl" to (selectedImageUri?.toString() ?: it.photoUrl.toString())
            )
            userRef.update(updatedData)
                .addOnSuccessListener {
                    Log.d("EditProfile", "Firestore 업데이트 성공")
                }
                .addOnFailureListener { e ->
                    Log.e("EditProfile", "Firestore 업데이트 실패", e)
                }
        }
    }
}
