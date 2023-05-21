package com.erensekkeli.roomieconnect.adapters


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.FeedActivity
import com.erensekkeli.roomieconnect.databinding.MediaItemBinding
import com.erensekkeli.roomieconnect.databinding.SearchResultItemBinding
import com.erensekkeli.roomieconnect.fragments.MediaDetailFragment
import com.erensekkeli.roomieconnect.fragments.ProfileDetailFragment
import com.erensekkeli.roomieconnect.models.Media
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class MediaListAdapter(val userList: ArrayList<User>, var fragmentType: Int = 0): RecyclerView.Adapter<MediaListAdapter.MediaViewHolder>(){


    class MediaViewHolder(val binding: SearchResultItemBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = SearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }


    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.binding.nameSurnameResult.text = userList[position].name + " " + userList[position].surname
        val imageUri: Uri? = userList[position].profileImage?.toUri()
        if(imageUri != null) {
            Glide.with(holder.itemView.context).load(imageUri).into(holder.binding.profileImageResult)
        }else {
            Glide.with(holder.itemView.context).load(R.drawable.app_icon).into(holder.binding.profileImageResult)
        }
        holder.binding.statusResult.text = when(userList[position].status) {
            0 -> holder.itemView.context.getString(R.string.hi) + " " +  holder.itemView.context.getString(R.string.status_0)
            1 -> holder.itemView.context.getString(R.string.hi) + " " + holder.itemView.context.getString(R.string.status_1)
            2 -> holder.itemView.context.getString(R.string.hi) + " " + holder.itemView.context.getString(R.string.status_2)
            else -> holder.itemView.context.getString(R.string.hi) + " " + holder.itemView.context.getString(R.string.status_0)
        }
        holder.binding.departmentResult.text = userList[position].department
        holder.binding.gradeYearResult.text = userList[position].gradeYear.toString()
        holder.itemView.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("user", userList[position])
            val fragment = ProfileDetailFragment()
            fragment.arguments = bundle
            val transaction = (holder.itemView.context as FeedActivity).supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.enter_right_to_left,
                R.anim.exit_right_to_left,
                R.anim.enter_left_to_right,
                R.anim.exit_left_to_right
            )
            transaction.replace(R.id.feedContainerFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

    }

}