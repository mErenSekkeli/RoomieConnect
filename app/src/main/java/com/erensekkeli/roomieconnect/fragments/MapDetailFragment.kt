package com.erensekkeli.roomieconnect.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.Settings
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
import com.erensekkeli.roomieconnect.models.User
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
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
    private var studentStatus: Int? = null
    private var houseMarker: Marker? = null
    private var userMarker: Marker? = null
    private var polyLine: Polyline? = null
    private var mapFragment: SupportMapFragment? = null
    private var allHouseMarker = ArrayList<Marker>()
    private lateinit var googleMap: GoogleMap
    private var distanceFilter: Int? = null
    private val ytuCampus = LatLng(41.02612590935019, 28.88995205632831)
    private lateinit var fragmentContext: Context

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        var userLocation: GeoPoint? = null
        firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get().addOnSuccessListener {
            val document = it.documents[0]
            userLocation = document.getGeoPoint("location")

            if(userLocation != null) {
                latitude = userLocation!!.latitude
                longitude = userLocation!!.longitude
                val userLatLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                userMarker = googleMap.addMarker(MarkerOptions().position(userLatLng).title(fragmentContext.getString(R.string.your_location)).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker)))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } else {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        userMarker = googleMap.addMarker(MarkerOptions().position(userLatLng).title(fragmentContext.getString(R.string.your_location)).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker)))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }else {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                }
            }

            googleMap.addMarker(MarkerOptions().position(ytuCampus).title(fragmentContext.getString(R.string.ytu)))?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.school_marker))

            polyLine = googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .add(
                        ytuCampus,
                        LatLng(latitude, longitude)
                    )
            )
        }
        getHouses(googleMap)
        locationManager = activity?.getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            if(userMarker != null) {
                userMarker!!.remove()
            }
            if (polyLine != null) {
                polyLine!!.remove()
            }
            //update user location on firestore
            firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get().addOnSuccessListener {
                val document = it.documents[0]
                document.reference.update("location", GeoPoint(location.latitude, location.longitude))
            }

            val userLocation = LatLng(location.latitude, location.longitude)
            val bitmapDescriptor = context?.let {
                BitmapDescriptorFactory.fromResource(R.drawable.user_marker)
            }
            userMarker = googleMap.addMarker(MarkerOptions().position(userLocation).title(fragmentContext.getString(R.string.your_location)).icon(bitmapDescriptor))

            polyLine = googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .add(
                        LatLng(41.02612590935019, 28.88995205632831),
                        LatLng(location.latitude, location.longitude)
                    )
            )
        }

        if(studentStatus == 2) {
            googleMap.setOnMapLongClickListener {
                if(houseMarker != null) {
                    houseMarker!!.remove()
                }
                houseMarker = googleMap.addMarker(MarkerOptions().position(it).title(fragmentContext.getString(R.string.house)).icon(BitmapDescriptorFactory.fromResource(R.drawable.house_icon)))
                if(userMarker != null) {
                    binding.setHouseLocationBtn.visibility = View.VISIBLE
                }
            }
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

        googleMap.setOnMarkerClickListener { marker ->
            if(marker.title != fragmentContext.getString(R.string.your_location) && marker.title != fragmentContext.getString(R.string.house) && marker.title != fragmentContext.getString(R.string.ytu)) {
                val userEmail = marker.title
                firestore.collection("UserData").whereEqualTo("email", userEmail)
                    .get().addOnSuccessListener {
                        val document = it.documents[0]
                        val email = document.getString("email")
                        val name = document.getString("name")
                        val surname = document.getString("surname")
                        val fcmToken = document.get("fcmToken") as String?
                        val contactMail = document.getString("contactMail")
                        val contactPhone = document.getString("contactPhone")
                        val department = document.getString("department")
                        val status = document.getLong("status")?.toInt()
                        val campusDistance = document.getLong("campusDistance")?.toInt()
                        val gradeYear = document.getLong("gradeYear")?.toInt()
                        val homeTime = document.getLong("homeTime")?.toInt()
                        val profileImage = document.getString("profileImage")
                        val user = User(email, name!!, surname!!, fcmToken, contactMail, contactPhone, department, status, profileImage, campusDistance, gradeYear, homeTime)
                        val bundle = Bundle()

                        if(studentStatus == 1)
                            bundle.putBoolean("isMatchRequest", true)

                        bundle.putSerializable("user", user)
                        val fragment = ProfileDetailFragment()
                        fragment.arguments = bundle
                        val transaction = activity?.supportFragmentManager?.beginTransaction()
                        transaction?.setCustomAnimations(
                            R.anim.enter_right_to_left,
                            R.anim.exit_right_to_left,
                            R.anim.enter_left_to_right,
                            R.anim.exit_left_to_right
                        )
                        transaction?.replace(R.id.feedContainerFragment, fragment)
                        transaction?.addToBackStack(null)
                        transaction?.commit()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                    }
            }else {
                if(marker.title == fragmentContext.getString(R.string.your_location)) {
                    Toast.makeText(fragmentContext, R.string.your_location, Toast.LENGTH_SHORT).show()
                }else if(marker.title == fragmentContext.getString(R.string.house)) {
                    Toast.makeText(fragmentContext, R.string.house, Toast.LENGTH_SHORT).show()
                }else {
                    Toast.makeText(fragmentContext, R.string.ytu, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
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
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(callback)

        //get status from bundle
        val bundle = arguments
        if(bundle != null) {
            studentStatus = bundle.getInt("studentStatus")
            if(studentStatus == 2) {
                Toast.makeText(fragmentContext, R.string.map_detail_long_press_2, Toast.LENGTH_LONG).show()
            }else {
                Toast.makeText(fragmentContext, R.string.map_detail_long_press_1, Toast.LENGTH_LONG).show()
            }
            distanceFilter = bundle.getInt("distanceFilter")
        }


        binding.backBtn.setOnClickListener {
            getBack()
        }

        binding.setHouseLocationBtn.setOnClickListener {
            setHouseLocation()
        }
    }

    private fun getBack() {
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
    }

    private fun getHouses(googleMap: GoogleMap) {
        if(allHouseMarker.size > 0) {
            for(marker in allHouseMarker) {
                marker.remove()
            }
        }
        var collection = firestore.collection("UserData").whereEqualTo("status", 2)

        if(distanceFilter != null && distanceFilter!! > 0) {
            collection = collection.whereLessThanOrEqualTo("campusDistance", distanceFilter!!)
        }

        collection.get().addOnSuccessListener { result ->
            for (document in result) {
                val location = document.getGeoPoint("houseLocation")
                val userEmail = document.getString("email")
                if (location != null) {
                    val houseLatLng = LatLng(location.latitude, location.longitude)
                    val marker= googleMap.addMarker(MarkerOptions().position(houseLatLng).title(userEmail).icon(BitmapDescriptorFactory.fromResource(R.drawable.house_icon)))
                    if(marker != null)
                        allHouseMarker.add(marker)
                }
            }
        }
    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result) {
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, locationListener)
                }
            } else {
                Toast.makeText(fragmentContext, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setHouseLocation() {
        if (houseMarker == null) {
            Toast.makeText(fragmentContext, R.string.set_house_location, Toast.LENGTH_LONG).show()
            return
        }
        val collection =
            firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email)

        val loc1 = Location("")
        loc1.latitude = ytuCampus.latitude
        loc1.longitude = ytuCampus.longitude

        val loc2 = Location("")
        loc2.latitude = houseMarker!!.position.latitude
        loc2.longitude = houseMarker!!.position.longitude

        val distance: Float = loc1.distanceTo(loc2)

        collection.get().addOnSuccessListener { result ->
            val document = result.documents[0]
            document.reference.update(
                "houseLocation",
                GeoPoint(houseMarker!!.position.latitude, houseMarker!!.position.longitude)
            ).addOnSuccessListener {
                getHouses(googleMap)
                document.reference.update("campusDistance", distance.toInt())
                Toast.makeText(fragmentContext, R.string.house_location_set, Toast.LENGTH_LONG).show()
            }
            binding.setHouseLocationBtn.visibility = View.GONE
        }
    }
}