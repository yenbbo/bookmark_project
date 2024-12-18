package com.example.book_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.book_project.databinding.ActivityNotificationBinding
import com.example.book_project.databinding.ItemNotificationBinding

class NotiViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

class NotiAdapter(val data: MutableList<String>) : RecyclerView.Adapter<NotiViewHolder>(){
    override fun getItemCount(): Int = data.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotiViewHolder =
        NotiViewHolder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NotiViewHolder, position: Int){
        holder.binding.itemText.text = data[position]
    }
}

class NotificationActivity : AppCompatActivity() {
    val binding: ActivityNotificationBinding by lazy {
        ActivityNotificationBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val data = mutableListOf<String>()
        for(i in 1..10){
            data.add("Notification $i")
        }
        binding.notificationRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.notificationRecyclerview.adapter = NotiAdapter(data)
        binding.notificationRecyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        binding.beforeIcon.setOnClickListener{
            finish()
        }
    }
}