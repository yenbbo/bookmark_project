package com.example.book_project

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.book_project.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 버튼 클릭 시 로그인 요청
        binding.btnKaKaoLogin.setOnClickListener{
            kakaoLogin()
        }
    }

    private fun kakaoLogin() {
        // 카카오계정으로 로그인 공통 callback 구성
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                requestUserInfo()
            }
        }

        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                    requestUserInfo()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun requestUserInfo() {
        // 카카오 로그인 후 사용자 정보 요청
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            } else if (user != null) {
                val userID = user.id.toString()
                val userName = user.kakaoAccount?.profile?.nickname
                val userProfileImage = user.kakaoAccount?.profile?.thumbnailImageUrl
                Log.i(TAG, "사용자 정보: $userName, $userProfileImage")

                // Firebase에 사용자 정보 저장
                val userInfo = mapOf("id" to userID, "name" to userName, "profileImage" to userProfileImage)
                firebaseAuth(userID, userInfo)

                // 사용자 정보를 OneActivity로 전달
                val intent = Intent(this, OneActivity::class.java).apply {
                    putExtra("userName", userName)
                    putExtra("userProfileImage", userProfileImage)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun firebaseAuth(userID: String, userInfo: Map<String, Any?>) {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("users").document(userID)
            .set(userInfo)
            .addOnSuccessListener {
                Log.i(TAG, "saving user info success")
            }
            .addOnFailureListener {
                Log.e(TAG, "saving user info failure", it)
            }

    }

}