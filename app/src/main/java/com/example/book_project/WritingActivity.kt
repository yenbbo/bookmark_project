package com.example.book_project

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.book_project.databinding.ActivityWritingBinding
import java.io.File

class WritingActivity : AppCompatActivity() {
    val binding: ActivityWritingBinding by lazy {
        ActivityWritingBinding.inflate(layoutInflater)
    }

    private var imageUri: Uri? = null
    private lateinit var requestCameraFileLaunch: ActivityResultLauncher<Intent>
    private lateinit var requestGalleryFileLaunch: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val editText = binding.pageNum
        editText.setText("p.")

        editText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().startsWith("p.")) {
                    editText.setText("p.")
                    editText.setSelection(editText.text.length) // 커서 마지막으로 이동
                }
                else {
                    val input = s.toString().substring(2)
                    val filteredInput = input.filter { it.isDigit() }
                    if (input != filteredInput) {
                        editText.setText("p.$filteredInput")
                        editText.setSelection(editText.text.length)
                    }
                }
            }
        })
        // x 버튼 클릭
        binding.close.setOnClickListener {
            finish()
        }
        // 등록 버튼 클릭하면 글 등록(CommentActivity에)
        binding.submit.setOnClickListener {
            val content = binding.content.text.toString()
            val page = binding.pageNum.text.toString()

            if (content.isNotEmpty() && page != "p." && page.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("content", content)
                resultIntent.putExtra("page", page)

                imageUri?.let {
                    resultIntent.putExtra("image", it.toString())
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            else {
                // 둘 중 하나라도 입력 안했을 때
                if (content.isEmpty()) {
                    binding.content.error = "내용을 입력해주세요."
                }
                if (page == "p." || page.isEmpty()) {
                    binding.pageNum.error = "페이지를 입력해주세요."
                }
            }
        }
        // 카메라 launcher 설정
        requestCameraFileLaunch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                imageUri?.let { uri ->
                    val imageView = binding.image
                    imageView.setImageURI(uri)
                }
            }
        }
        // 갤러리 launcher 설정
        requestGalleryFileLaunch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val selectedImageUri: Uri? = it.data?.data
                val imageView = binding.image
                imageView.setImageURI(selectedImageUri)
                imageUri = selectedImageUri
            }
        }

        // 카메라 버튼 클릭
        binding.camera.setOnClickListener {
            val options = arrayOf("카메라", "앨범에서 사진 선택")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("사진 추가하기")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()  // 카메라 호출
                    1 -> openGallery()  // 갤러리 호출
                }
            }
            builder.show()
        }
    }


    private fun openCamera(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri() // 이미지 저장할 URI 생성
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        requestCameraFileLaunch.launch(cameraIntent)
    }

    private fun openGallery(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        requestGalleryFileLaunch.launch(galleryIntent)
    }

    private fun createImageUri(): Uri {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_image.jpg")
        return FileProvider.getUriForFile(this, "com.example.book_project.fileprovider", file)
    }
}