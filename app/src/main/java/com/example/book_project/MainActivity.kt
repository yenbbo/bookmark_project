package com.example.book_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.book_project.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(binding.root)

        // Firebase 인증 초기화
        auth = FirebaseAuth.getInstance()

        // 로그인 상태 체크
        val user = auth.currentUser
        if (user != null) {
            // 이미 로그인 되어 있으면 OneActivity로 이동
            moveActivity(user)
        }

        // GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Firebase 콘솔에서 제공되는 웹 클라이언트 ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 버튼 클릭 시 Google 로그인
        binding.btnGoogleLogin.setOnClickListener {
            googleLogin()
        }
    }

    private fun googleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: Exception) {
                Log.w("GoogleSignIn", "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleSignIn", "signInWithCredential:success")
                    val user = auth.currentUser
                    // 구글 사용자 정보를 다른 Activity로 전달
                    moveActivity(user)
                } else {
                    Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun moveActivity(user: FirebaseUser?){
        val intent = Intent(this, OneActivity::class.java).apply {
            putExtra("userName", user?.displayName)
            putExtra("userEmail", user?.email)
            putExtra("userProfileImage", user?.photoUrl.toString())
        }
        startActivity(intent)
        finish()
    }
}
