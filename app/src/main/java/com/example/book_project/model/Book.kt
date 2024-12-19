package com.example.book_project.model

import android.os.Parcel
import android.os.Parcelable

//검색했을 때 나올 아이템
data class Book (
    val id: String,
    val title: String,      // 책 제목
    val author: String,     // 작가
    val coverUrl: String, // URL로 이미지를 불러올 수 있도록 함
    val description: String, //책 설명
    val rating: Float,
    val year: Int,
    val publisher: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readString().toString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(coverUrl)
        parcel.writeString(description)
        parcel.writeFloat(rating)
        parcel.writeInt(year)
        parcel.writeString(publisher)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book {
            return Book(parcel)
        }

        override fun newArray(size: Int): Array<Book?> {
            return arrayOfNulls(size)
        }
    }
}
