package com.example.book_project.model

import android.os.Parcel
import android.os.Parcelable

//검색했을 때 나올 아이템
data class Book(
    var id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val description: String = "",
    var rating: Float = 0f,
    val year: Int = 0,
    val publisher: String = "",
    var ratings: Map<String, Float> = mapOf()) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readHashMap(String::class.java.classLoader) as Map<String, Float>    ) {
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(coverUrl)
        parcel.writeString(description)
        parcel.writeInt(year)
        parcel.writeString(publisher)
        parcel.writeMap(ratings)
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
