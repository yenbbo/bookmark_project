package com.example.book_project

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility

class GlobalApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        val keyHash = Utility.getKeyHash(this)
        Log.d("Hash", keyHash)
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }
}