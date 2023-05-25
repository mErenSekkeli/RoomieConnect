package com.erensekkeli.roomieconnect.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.adapters.SearchResultAdapter
import com.erensekkeli.roomieconnect.databinding.FragmentSearchResultBinding
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchResultFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private var userList: ArrayList<User> = ArrayList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        firestore = Firebase.firestore
        recyclerView = binding.searchResultItemList
        binding.searchResultItemList.layoutManager = LinearLayoutManager(context)
        binding.searchResultItemList.adapter = SearchResultAdapter(userList)

        val campusDistanceMin = arguments?.getInt("campusDistanceMin")
        val campusDistanceMax = arguments?.getInt("campusDistanceMax")
        val numberOfDaysMin = arguments?.getInt("numberOfDaysMin")
        val numberOfDaysMax = arguments?.getInt("numberOfDaysMax")
        val statusIndex= arguments?.getInt("statusIndex")
        getProcessAnimation()

        if(campusDistanceMin == null && campusDistanceMax == null && numberOfDaysMin == null && numberOfDaysMax == null && statusIndex == null) {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            removeProcessAnimation()
            getBack()
            return
        }

        var collection: Query = firestore.collection("UserData")


        if(campusDistanceMin != null && campusDistanceMin != 0) {
            collection = collection.whereGreaterThanOrEqualTo("campusDistance", campusDistanceMin)
        }
        if(campusDistanceMax != null && campusDistanceMax != 0) {
            collection = collection.whereLessThanOrEqualTo("campusDistance", campusDistanceMax)
        }
        if(numberOfDaysMin != null && numberOfDaysMin != 0) {
            collection = collection.whereGreaterThanOrEqualTo("homeTime", numberOfDaysMin)
        }
        if(numberOfDaysMax != null && numberOfDaysMax != 0) {
            collection = collection.whereLessThanOrEqualTo("homeTime", numberOfDaysMax)
        }
        if(statusIndex != null) {
            collection = collection.whereEqualTo("status", statusIndex)
        }

        collection.get().addOnSuccessListener { documents ->
            if(documents.isEmpty) {
                Toast.makeText(context, R.string.no_result, Toast.LENGTH_SHORT).show()
                removeProcessAnimation()
                getBack()
                return@addOnSuccessListener
            }
            for(document in documents) {
                val email = document.getString("email")
                val name = document.getString("name")
                val surname = document.getString("surname")
                val contactMail = document.getString("contactMail")
                val contactPhone = document.getString("contactPhone")
                val department = document.getString("department")
                val status = document.getLong("status")?.toInt()
                val campusDistance = document.getLong("campusDistance")?.toInt()
                val gradeYear = document.getLong("gradeYear")?.toInt()
                val homeTime = document.getLong("homeTime")?.toInt()
                val profileImage = document.getString("profileImage")

                val user = User(email, name!!, surname!!, contactMail, contactPhone, department, status, profileImage, campusDistance, gradeYear, homeTime)
                userList.add(user)
            }
            binding.searchResultItemList.adapter?.notifyDataSetChanged()
            removeProcessAnimation()
        }.addOnFailureListener { exception ->
            Log.d("SearchResultFragment", exception.localizedMessage!!)
            Toast.makeText(context, exception.localizedMessage, Toast.LENGTH_SHORT).show()
            removeProcessAnimation()
            getBack()
        }

        binding.backBtn.setOnClickListener {
            getBack()
        }

    }

    override fun onResume() {
        super.onResume()
        //delete previous items
        userList.clear()
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
}