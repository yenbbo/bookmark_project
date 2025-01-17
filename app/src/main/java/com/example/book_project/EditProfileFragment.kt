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

        // Firestore에서 사용자 정보 불러오기
        currentUser?.let {
            val userRef = db.collection("users").document(it.uid)
            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("userName")
                    val userPhotoUrl = document.getString("userProfileImage")

                    // 사용자 이름 설정
                    binding.editUserName.setText(userName)

                    // 프로필 사진 설정
                    Glide.with(this)
                        .load(userPhotoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.profile_icon)
                        .into(binding.editProfileImage)
                }
            }
        }

        // 프로필 이미지 변경 버튼 클릭 이벤트
        binding.changeProfileImageButton.setOnClickListener {
            openGallery()
        }

        // 저장 버튼 클릭 이벤트
        binding.saveProfileButton.setOnClickListener {
            saveProfile()
            parentFragmentManager.popBackStack() // 뒤로가기
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
            val photoUri = selectedImageUri ?: it.photoUrl
            val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(newUserName)
                .setPhotoUri(photoUri)
                .build()

            it.updateProfile(userProfileChangeRequest)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userRef = db.collection("users").document(it.uid)
                        val updatedData = hashMapOf<String, Any>(
                            "name" to newUserName,
                            "photoUrl" to (selectedImageUri?.toString() ?: "")
                        )
                        userRef.update(updatedData)
                            .addOnSuccessListener {
                                Log.d("EditProfile", "Firestore 업데이트 성공")
                                Toast.makeText(requireContext(), "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                                // 프로필 업데이트 결과 전달
                                parentFragmentManager.setFragmentResult("profile", Bundle().apply {
                                    putString("name", newUserName)
                                    putString("photoUrl", selectedImageUri?.toString())
                                })
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditProfile", "Firestore 업데이트 실패", e)
                            }

                    } else {
                        Toast.makeText(requireContext(), "프로필 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}