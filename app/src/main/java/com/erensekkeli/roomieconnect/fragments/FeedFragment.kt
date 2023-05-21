package com.erensekkeli.roomieconnect.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.adapters.MediaListAdapter
import com.erensekkeli.roomieconnect.databinding.FragmentFeedBinding
import com.erensekkeli.roomieconnect.models.Media
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FeedFragment : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private var userList: ArrayList<User> = ArrayList()
    private var userStatus: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        auth = Firebase.auth
        firestore = Firebase.firestore
        recyclerView = binding.mediaRecyclerView
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.setHasFixedSize(false);
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MediaListAdapter(userList, 0)

        binding.addNewMediaButton.setOnClickListener {
            goToCreateMedia(view)
        }
        getProcessAnimation()

        firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email!!)
            .get().addOnSuccessListener {
                val document = it.documents[0]
                userStatus = document.getLong("status")?.toInt() ?: 0
                var collection: Query = firestore.collection("UserData")
                if(userStatus == 1) {
                    collection = collection.whereEqualTo("status", 2)
                }else if (userStatus == 2) {
                    collection = collection.whereEqualTo("status", 1)
                }

                collection.get().addOnSuccessListener { documents ->

                    if(documents != null) {
                        for(document in documents) {
                            val name = document.getString("name") ?: ""
                            val surname = document.getString("surname") ?: ""
                            val contactMail = document.getString("contactMail") ?: ""
                            val contactPhone = document.getString("contactPhone") ?: ""
                            val department = document.getString("department") ?: "-"
                            val status = document.getLong("status")?.toInt() ?: 0
                            val campusDistance = document.getLong("campusDistance")?.toInt() ?: 0
                            val gradeYear = document.getLong("gradeYear")?.toInt() ?: 0
                            val homeTime = document.getLong("homeTime")?.toInt() ?: 0
                            val profileImage = document.getString("profileImage")

                            val user = User(name!!, surname!!, contactMail, contactPhone, department, status, profileImage, campusDistance, gradeYear, homeTime)
                            userList.add(user)
                        }
                        recyclerView.adapter?.notifyDataSetChanged()
                        removeProcessAnimation()
                    }
                }
            }


    }

    private fun getProcessAnimation() {
        binding.progressContainer.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        getProcessAnimation()
        userList.clear()
    }

    private fun removeProcessAnimation() {
        binding.progressContainer.visibility = View.INVISIBLE
    }

    private fun goToCreateMedia(view: View) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.enter_right_to_left,
            R.anim.exit_right_to_left,
            R.anim.enter_left_to_right,
            R.anim.exit_left_to_right
        )
        transaction.replace(R.id.feedContainerFragment, CreateMediaFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }


}