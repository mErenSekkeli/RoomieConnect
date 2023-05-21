package com.erensekkeli.roomieconnect.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.databinding.FragmentMapDetailBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MapDetailFragment : Fragment() {

    private lateinit var binding: FragmentMapDetailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var latitude = 0.0
    private var longitude = 0.0

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        var userLocation: GeoPoint? = null
        firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get().addOnSuccessListener {
            val document = it.documents[0]
            userLocation = document.getGeoPoint("location")

            if(userLocation != null) {
                latitude = userLocation!!.latitude
                longitude = userLocation!!.longitude
                val userLatLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                googleMap.addMarker(MarkerOptions().position(userLatLng).title(requireContext().getString(R.string.your_location)).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker)))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } else {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.addMarker(MarkerOptions().position(userLatLng).title(requireContext().getString(R.string.your_location)).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker)))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            }

            val ytuCampus = LatLng(41.02612590935019, 28.88995205632831)
            googleMap.addMarker(MarkerOptions().position(ytuCampus).title(requireContext().getString(R.string.ytu)))?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.school_marker))

            googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .add(
                        LatLng(41.02612590935019, 28.88995205632831),
                        LatLng(latitude, longitude)
                    )
            )
        }

        val collection = firestore.collection("UserData").whereEqualTo("status", 2)

        collection.get().addOnSuccessListener { result ->
            for(document in result) {
                val location = document.getGeoPoint("location")
                val userLocation = LatLng(location!!.latitude, location.longitude)
                googleMap.addMarker(MarkerOptions().position(userLocation).title(requireContext().getString(R.string.house)))?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.house_icon))
            }
        }

        locationManager = activity?.getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->

            googleMap.clear()
            val userLocation = LatLng(location.latitude, location.longitude)
            val bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.user_marker)
            googleMap.addMarker(MarkerOptions().position(userLocation).title(requireContext().getString(R.string.your_location)).icon(bitmapDescriptor))

            val collection = firestore.collection("UserData").whereEqualTo("status", 2)

            collection.get().addOnSuccessListener { result ->
                for(document in result) {
                    val location = document.getGeoPoint("location")
                    val userLocation = LatLng(location!!.latitude, location.longitude)
                    googleMap.addMarker(MarkerOptions().position(userLocation).title(requireContext().getString(R.string.house)))?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.house_icon))
                }
            }
            googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .add(
                        LatLng(41.02612590935019, 28.88995205632831),
                        LatLng(location.latitude, location.longitude)
                    )
            )
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(binding.root, R.string.give_location_permission, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.give_permission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, locationListener)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMapDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        firestore = Firebase.firestore
        registerLauncher()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(callback)

        binding.backBtn.setOnClickListener {
            getBack()
        }
    }

    private fun getBack() {
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result) {
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, locationListener)
                }
            } else {
                Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }
}