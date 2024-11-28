package com.example.book_project

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.book_project.databinding.FragmentHomeBinding
import com.example.book_project.databinding.FragmentMyPageBinding

class MyPageFragment : Fragment() {
    lateinit var binding: FragmentMyPageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.myBookshelf.setOnClickListener() {
            val intent = Intent(requireContext(), MyBookshelfActivity::class.java)
            startActivity(intent)
        }
        binding.myPost.setOnClickListener() {
            val intent = Intent(requireContext(), MyPostsActivity::class.java)
            startActivity(intent)
        }
    }
}