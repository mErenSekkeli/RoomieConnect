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

        val nameSurname: String? = arguments?.getString("nameSurname")
        var name: String? = null
        var surname: String? = null

        getProcessAnimation()

        if(nameSurname != null && nameSurname.contains(" ")) {
            val lastSpaceIndex = nameSurname.lastIndexOf(" ")
            name = nameSurname.substring(0, lastSpaceIndex)
            surname = nameSurname.substring(lastSpaceIndex + 1)
        }

        var country: String? = arguments?.getString("country")
        var city: String? = arguments?.getString("city")

        val graduateDate: String? = arguments?.getString("graduateDate")

        if(nameSurname == null && country == null && city == null && graduateDate == null) {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            removeProcessAnimation()
            getBack()
            return
        }

        var collection: Query = firestore.collection("UserData")


        if(name != null && surname != null) {
            collection = collection.whereEqualTo("name", name)

            collection = collection.whereEqualTo("surname", surname)
        } else if(nameSurname != null) {
            collection = collection.whereEqualTo("name", nameSurname)
        }

        if(country != null) {
            collection = collection.whereEqualTo("country", country)
        }
        if(city != null) {
            collection = collection.whereEqualTo("city", city)
        }
        if(graduateDate != null) {
            collection = collection.whereEqualTo("graduationDate", graduateDate)
        }

        collection.get().addOnSuccessListener { documents ->
            if(documents.isEmpty) {
                Toast.makeText(context, R.string.no_result, Toast.LENGTH_SHORT).show()
                removeProcessAnimation()
                getBack()
                return@addOnSuccessListener
            }
            for(document in documents) {
                val name = document.getString("name")
                val surname = document.getString("surname")
                val contactMail = document.getString("contactMail")
                val contactPhone = document.getString("contactPhone")
                val department = document.getString("department")
                val status = document.getString("status").toString().toInt()
                val campusDistance = document.getString("campusDistance").toString().toInt()
                val gradeYear = document.getString("gradeYear").toString().toInt()
                val homeTime = document.getString("homeTime").toString().toInt()
                val profileImage = document.getString("profileImage")

                val user = User(name!!, surname!!, contactMail, contactPhone, department, status, profileImage, campusDistance, gradeYear, homeTime)
                userList.add(user)
            }
            binding.searchResultItemList.adapter?.notifyDataSetChanged()
            removeProcessAnimation()
        }.addOnFailureListener { exception ->
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
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