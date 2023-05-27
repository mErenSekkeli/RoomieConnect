package com.erensekkeli.roomieconnect.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.erensekkeli.roomieconnect.fragments.AnnouncementFragment
import com.erensekkeli.roomieconnect.fragments.FeedFragment
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.fragments.SearchFragment
import com.erensekkeli.roomieconnect.databinding.ActivityFeedBinding
import com.erensekkeli.roomieconnect.fragments.MapFragment
import com.erensekkeli.roomieconnect.fragments.ProfileDetailFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firestore = Firebase.firestore

        replaceFragment(FeedFragment(), "FeedFragment")

        val isNotification = intent.getBooleanExtra("requestSent", false)
        if(isNotification) {
            replaceFragment(AnnouncementFragment(), "AnnouncementFragment")
        }

        binding.bottomNavigationBar.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.home -> {
                    replaceFragment(FeedFragment(), "FeedFragment")
                }
                R.id.map -> {
                    replaceFragment(MapFragment(), "MapFragment")
                }
                R.id.search -> {
                    replaceFragment(SearchFragment(), "SearchFragment")
                }
                R.id.profile -> {
                    replaceFragment(ProfileDetailFragment(), "ProfileFragment")
                }
                else -> {
                    replaceFragment(FeedFragment(), "FeedFragment")
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.enter_right_to_left,
            R.anim.exit_right_to_left,
            R.anim.enter_left_to_right,
            R.anim.exit_left_to_right
        )
        val bundle = Bundle()
        bundle.putString("tag", tag)
        fragment.arguments = bundle
        fragmentTransaction.replace(R.id.feedContainerFragment, fragment)
        fragmentTransaction.commit()
    }

}