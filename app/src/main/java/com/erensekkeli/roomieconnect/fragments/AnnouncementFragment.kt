package com.erensekkeli.roomieconnect.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.adapters.RequestListAdapter
import com.erensekkeli.roomieconnect.databinding.FragmentAnnouncementBinding
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class AnnouncementFragment : Fragment() {

    private lateinit var binding: FragmentAnnouncementBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private var userList: ArrayList<User> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAnnouncementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        firestore = Firebase.firestore
        recyclerView = binding.RequestItemList
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RequestListAdapter(userList)

        getProcessAnimation()

        firestore.collection("MatchRequests").whereEqualTo("receiver", auth.currentUser!!.email).whereEqualTo("requestStatus", 0).get()
            .addOnSuccessListener { result->
                if(result.isEmpty) {
                    Toast.makeText(context, R.string.no_result, Toast.LENGTH_SHORT).show()
                    removeProcessAnimation()
                    getBack()
                    return@addOnSuccessListener
                }else {
                    val documents = result.documents
                    for(doc in documents) {
                        val senderEmail = doc.getString("sender")
                        val requestStatus = doc.getLong("requestStatus")?.toInt()

                        firestore.collection("UserData").whereEqualTo("email", senderEmail).get()
                            .addOnSuccessListener {
                                val document = it.documents[0]
                                val email = document.getString("email") ?: ""
                                val name = document.getString("name") ?: ""
                                val surname = document.getString("surname") ?: ""
                                val fcmToken = document.get("fcmToken") as String?
                                val contactMail = document.getString("contactMail") ?: ""
                                val contactPhone = document.getString("contactPhone") ?: ""
                                val department = document.getString("department") ?: "-"
                                val status = document.getLong("status")?.toInt() ?: 0
                                val campusDistance = document.getLong("campusDistance")?.toInt() ?: 0
                                val gradeYear = document.getLong("gradeYear")?.toInt() ?: 0
                                val homeTime = document.getLong("homeTime")?.toInt() ?: 0
                                val profileImage = document.getString("profileImage")

                                val user = User(email, name, surname, fcmToken, contactMail, contactPhone, department, status, profileImage, campusDistance, gradeYear, homeTime)
                                userList.add(user)
                                recyclerView.adapter?.notifyDataSetChanged()
                            }.addOnFailureListener {
                                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                                removeProcessAnimation()
                                getBack()
                            }
                    }
                    removeProcessAnimation()
                }

            }.addOnFailureListener {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                removeProcessAnimation()
                getBack()
            }

        binding.backBtn.setOnClickListener {
            getBack()
        }
    }

    private fun getProcessAnimation() {
        binding.progressContainer.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        userList.clear()
    }

    private fun removeProcessAnimation() {
        binding.progressContainer.visibility = View.INVISIBLE
    }

    private fun getBack() {
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
    }
}