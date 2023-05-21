package com.erensekkeli.roomieconnect.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.databinding.FragmentMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestedPermission: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        registerLauncher()
        auth = Firebase.auth
        firestore = Firebase.firestore
        binding.updateHouseLocationButton.setOnClickListener {
            updateHouseLocation(view)
        }
        binding.findHouseMapButton.setOnClickListener {
            getMap()
        }
    }

    private fun registerLauncher() {

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK) {
                checkLocationPermission(binding.root)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result) {
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if(location != null) {
                            val latitude = location.latitude
                            val longitude = location.longitude

                            saveLocationToFirebase(latitude, longitude)
                        }
                    }
                }
            }else {
                Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationPermission(view: View) {
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if(location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    saveLocationToFirebase(latitude, longitude)
                }else {
                    val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activityResultLauncher.launch(intent)
                }
            }
        }else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(view, R.string.give_location_permission, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.give_permission) {
                        requestedPermission = Manifest.permission.ACCESS_FINE_LOCATION
                        permissionLauncher.launch(requestedPermission)

                    }.show()
            }else {
                requestedPermission = Manifest.permission.ACCESS_FINE_LOCATION
                permissionLauncher.launch(requestedPermission)
            }
        }
    }

    private fun saveLocationToFirebase(latitude: Double, longitude: Double) {
        val currentUser = auth.currentUser
        if(currentUser != null) {
            val location = GeoPoint(latitude, longitude)
            firestore.collection("UserData").whereEqualTo("email", currentUser.email).get().addOnSuccessListener { documents ->
                val document = documents.documents[0]
                val documentId = document.id
                firestore.collection("UserData").document(documentId).update("location", location).addOnSuccessListener {
                    Toast.makeText(context, R.string.location_updated, Toast.LENGTH_LONG).show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(context, R.string.location_update_failed, Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun getMap() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.enter_right_to_left,
            R.anim.exit_right_to_left,
            R.anim.enter_left_to_right,
            R.anim.exit_left_to_right
        )
        transaction.replace(R.id.feedContainerFragment, MapDetailFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }


    private fun updateHouseLocation(view: View) {
        checkLocationPermission(view)
    }
}