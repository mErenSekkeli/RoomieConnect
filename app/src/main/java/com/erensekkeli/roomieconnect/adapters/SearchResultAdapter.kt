package com.erensekkeli.roomieconnect.adapters

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erensekkeli.roomieconnect.fragments.ProfileDetailFragment
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.FeedActivity
import com.erensekkeli.roomieconnect.databinding.SearchResultItemBinding
import com.erensekkeli.roomieconnect.models.User

class SearchResultAdapter(val userList: ArrayList<User>): RecyclerView.Adapter<SearchResultAdapter.UserHolder>() {

    class UserHolder(val binding: SearchResultItemBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val binding = SearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        holder.binding.nameSurnameResult.text = userList[position].name + " " + userList[position].surname
        val imageUri: Uri? = userList[position].profileImage?.toUri()
        if(imageUri != null) {
            Glide.with(holder.itemView.context).load(imageUri).into(holder.binding.profileImageResult)
        }else {
            Glide.with(holder.itemView.context).load(R.drawable.app_icon).into(holder.binding.profileImageResult)
        }
        holder.binding.contactPhoneResult.text = userList[position].contactPhone
        holder.binding.contactMailResult.text = userList[position].contactMail
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