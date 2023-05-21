package com.erensekkeli.roomieconnect.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.databinding.FragmentSearchBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class SearchFragment : Fragment() {


    private lateinit var binding: FragmentSearchBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        firestore = Firebase.firestore

        binding.button.setOnClickListener { searchUser(view) }
    }

    private fun searchUser(view: View) {
        //go to search result fragment
        hideKeyboard()
        val bundle = Bundle()
        val campusDistanceMin = binding.campusDistanceMinSearch?.text?.toString()?.toIntOrNull()
        if(campusDistanceMin != null) {
            bundle.putInt("campusDistanceMin", campusDistanceMin)
        }
        val campusDistanceMax = binding.campusDistanceMaxSearch?.text?.toString()?.toIntOrNull()
        if(campusDistanceMax != null) {
            bundle.putInt("campusDistanceMax", campusDistanceMax)
        }
        val statusIndex = resources.getStringArray(R.array.student_status).indexOf(binding.statusSpinner.selectedItem.toString())
        bundle.putInt("statusIndex", statusIndex)
        val numberOfDaysMin = binding.numberOfDaysMinSearch?.text?.toString()?.toIntOrNull()
        if(numberOfDaysMin != null) {
            bundle.putInt("numberOfDaysMin", numberOfDaysMin)
        }
        val numberOfDaysMax = binding.numberOfDaysMaxSearch?.text?.toString()?.toIntOrNull()
        if(numberOfDaysMax != null) {
            bundle.putInt("numberOfDaysMax", numberOfDaysMax)
        }
        val searchResultFragment = SearchResultFragment()
        searchResultFragment.arguments = bundle
        val transaction = parentFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.enter_right_to_left,
            R.anim.exit_right_to_left,
            R.anim.enter_left_to_right,
            R.anim.exit_left_to_right
        )
        transaction.replace(R.id.feedContainerFragment, searchResultFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

}