package com.erensekkeli.roomieconnect.fragments


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.adapters.ProfileDetailAdapter
import com.erensekkeli.roomieconnect.databinding.FragmentProfileDetailBinding
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileDetailFragment : Fragment() {

    private lateinit var binding: FragmentProfileDetailBinding
    private lateinit var user: User
    private lateinit var recyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        firestore = Firebase.firestore
        if(arguments?.getString("tag") == "ProfileFragment") {
            binding.backBtn.visibility = View.GONE
            binding.profileSettingsBtn.visibility = View.VISIBLE
            binding.profileSettingsBtn.setOnClickListener {
                val transaction = parentFragmentManager.beginTransaction()
                transaction.setCustomAnimations(
                    R.anim.enter_right_to_left,
                    R.anim.exit_right_to_left,
                    R.anim.enter_left_to_right,
                    R.anim.exit_left_to_right
                )
                transaction.replace(R.id.feedContainerFragment, ProfileSettingsFragment())
                transaction.addToBackStack(null)
                transaction.commit()
            }
            getData(object: UserDataCallback {
                override fun onUserDataLoaded(user: User) {
                    initializeData()
                }
            })
        }else{
            @Suppress("DEPRECATION")
            user = arguments?.getSerializable("user") as User
            initializeData()
        }

        binding.backBtn.setOnClickListener {
            getBack()
        }
    }

    private fun initializeData() {
        if(user.profileImage != null) {
            Glide.with(this).load(user.profileImage).into(binding.profileImage)
        }else {
            Glide.with(this).load(R.drawable.app_icon).into(binding.profileImage)
        }

        binding.profileNameSurname.text = user.name + " " + user.surname

        recyclerView = binding.profileDetailRecyclerView
        binding.profileDetailRecyclerView.layoutManager = LinearLayoutManager(context)
        val dataHashMap = HashMap<String, String?>()
        dataHashMap["Contact Mail"] = user.contactMail
        dataHashMap["Contact Phone"] = user.contactPhone
        dataHashMap["Department"] = user.department
        dataHashMap["Grade Year"] = user.gradeYear.toString()
        dataHashMap["Home Time"] = user.homeTime.toString()
        dataHashMap["Campus Distance"] = user.campusDistance.toString()
        dataHashMap["Status"] = user.status.toString()
        binding.profileDetailRecyclerView.adapter = ProfileDetailAdapter(dataHashMap)
    }

    private fun getData(callback: UserDataCallback) {
        getProcessAnimation()
        val usersCollection = firestore.collection("UserData")
        usersCollection.whereEqualTo("email", auth.currentUser!!.email!!).get().addOnSuccessListener { documents ->
            if(documents != null && !documents.isEmpty) {
                val document = documents.documents[0]
                val name = document.get("name") as String
                val surname = document.get("surname") as String
                val profilePicture = document.get("profileImage")?.toString() ?: ""
                val contactPhone = document.get("contactPhone")?.toString() ?: "-"
                val contactMail = document.get("contactMail")?.toString() ?: "-"
                val department = document.get("department")?.toString() ?: "-"
                val status = document.get("status")?.toString()?.toInt() ?: 0
                val campusDistance = document.get("campusDistance")?.toString()?.toInt() ?: 0
                val gradeYear = document.get("gradeYear")?.toString()?.toInt() ?: 0
                val homeTime = document.get("homeTime")?.toString()?.toInt() ?: 0
                user = User(name, surname, contactMail, contactPhone, department, status, profilePicture, campusDistance, gradeYear, homeTime)
                callback.onUserDataLoaded(user)
                removeProcessAnimation()
            }
        }.addOnFailureListener {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            user = User("","","","","",0,"",0,0,0)
            callback.onUserDataLoaded(user)
            removeProcessAnimation()
        }
    }
    private fun getBack() {
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
    }
    private fun getProcessAnimation() {
        binding.progressContainer.visibility = View.VISIBLE
    }

    private fun removeProcessAnimation() {
        binding.progressContainer.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        getData(object: UserDataCallback {
            override fun onUserDataLoaded(user: User) {
                initializeData()
            }
        })
    }
}

interface UserDataCallback {
    fun onUserDataLoaded(user: User)
}