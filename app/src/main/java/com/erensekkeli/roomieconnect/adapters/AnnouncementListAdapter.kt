package com.erensekkeli.roomieconnect.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.FeedActivity
import com.erensekkeli.roomieconnect.databinding.AnnouncementItemBinding
import com.erensekkeli.roomieconnect.fragments.ProfileDetailFragment
import com.erensekkeli.roomieconnect.models.Announcement
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date

class AnnouncementListAdapter(val announcements: ArrayList<Announcement>): RecyclerView.Adapter<AnnouncementListAdapter.AnnouncementViewHolder>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    class AnnouncementViewHolder(val binding: AnnouncementItemBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        auth = Firebase.auth
        firestore = Firebase.firestore
        val binding = AnnouncementItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnnouncementViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return announcements.size
    }

    private fun findStringDateIfDeadlinePast(date: String): Boolean {
        var simpleDateFormat: SimpleDateFormat

        if(date.contains("/")) {
            simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        } else {
            simpleDateFormat = SimpleDateFormat("dd.MM.yyyy")
        }
        val newDate: Date = simpleDateFormat.parse(date)
        val currentDate = Date()

        if(newDate.before(currentDate)) {
            return true
        }
        return false
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]
        if(findStringDateIfDeadlinePast(announcement.deadline.toString())) {
            //delete announcement
            val collection = firestore.collection("UserAnnouncements").whereEqualTo("email", announcement.email)
                .whereEqualTo("id", announcement.id)
            collection.get().addOnSuccessListener { documents ->
                if (documents.size() > 0) {
                    val document = documents.documents[0]
                    document.reference.delete()
                    announcements.removeAt(position)
                    notifyItemRemoved(position)
                }
            }.addOnFailureListener {
                Toast.makeText(holder.binding.root.context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            }
            return
        }
        holder.binding.title.text = announcement.title ?: "-"
        holder.binding.content.text = announcement.content ?: "-"
        holder.binding.deadline.text = announcement.deadline ?: "-"

        val collection = firestore.collection("UserData").whereEqualTo("email", announcement.email)
        collection.get().addOnSuccessListener { documents ->
            if (documents.size() > 0) {
                val document = documents.documents[0]
                val name = document.get("name").toString()
                val surname = document.get("surname").toString()
                holder.binding.creator.text = "$name $surname"
                holder.binding.creator.setOnClickListener {
                    val bundle = Bundle()
                    val user = User(name, surname,document.getString("contactMail"), document.getString("contactPhone"), document.getString("department"),
                        document.getLong("status")?.toInt(), document.getString("profileImage"), document.getLong("campusDistance")?.toInt(),
                        document.getLong("gradeYear")?.toInt(), document.getLong("homeTime")?.toInt())

                    bundle.putSerializable("user", user)
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
            }else {
                holder.binding.creator.text = "-"
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(holder.binding.root.context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
        }
    }

}