package com.erensekkeli.roomieconnect.fragments


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.NotificationParams
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.RemoteMessage.Notification
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

const val SERVER_KEY = "YOUR_SERVER_KEY"
class ProfileDetailFragment : Fragment() {

    private lateinit var binding: FragmentProfileDetailBinding
    private lateinit var user: User
    private lateinit var recyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseMessage: RemoteMessage
    private var isMatchRequest = false

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
            isMatchRequest = arguments?.getBoolean("isMatchRequest", false)!!
            if(isMatchRequest) {
                binding.sendMatchRequestBtn.visibility = View.VISIBLE
            }
        }

        binding.backBtn.setOnClickListener {
            getBack()
        }

        if(isMatchRequest){
            getProcessAnimation()
            firestore.collection("MatchRequests").whereEqualTo("sender", auth.currentUser!!.email).whereEqualTo("receiver", user.email).get()
                .addOnSuccessListener {
                    if(it.documents.size > 0) {
                        binding.sendMatchRequestBtn.visibility = View.GONE
                        binding.alreadySent.visibility = View.VISIBLE
                    }
                    removeProcessAnimation()
                }
                .addOnFailureListener {
                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    removeProcessAnimation()
                }

            binding.sendMatchRequestBtn.setOnClickListener {
                sendMatchRequest()
            }
        }

        binding.alreadySent.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.match_request)
            builder.setMessage(R.string.already_sent_match_request)
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
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
                val email = document.get("email") as String
                val name = document.get("name") as String
                val surname = document.get("surname") as String
                val fcmToken = document.get("fcmToken") as String?
                val profilePicture = document.get("profileImage")?.toString() ?: ""
                val contactPhone = document.get("contactPhone")?.toString() ?: "-"
                val contactMail = document.get("contactMail")?.toString() ?: "-"
                val department = document.get("department")?.toString() ?: "-"
                val status = document.get("status")?.toString()?.toInt() ?: 0
                val campusDistance = document.get("campusDistance")?.toString()?.toInt() ?: 0
                val gradeYear = document.get("gradeYear")?.toString()?.toInt() ?: 0
                val homeTime = document.get("homeTime")?.toString()?.toInt() ?: 0
                user = User(email, name, surname, fcmToken, contactMail, contactPhone, department, status, profilePicture, campusDistance, gradeYear, homeTime)
                callback.onUserDataLoaded(user)
                removeProcessAnimation()
            }
        }.addOnFailureListener {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            user = User("" ,"","", "","","","",0,"",0,0,0)
            callback.onUserDataLoaded(user)
            removeProcessAnimation()
        }
    }

    private fun sendMatchRequest() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(R.string.match_request)
        alertDialog.setMessage(R.string.match_request_message)
        alertDialog.setPositiveButton(R.string.yes) { _, _ ->
            val matchRequestsCollection = firestore.collection("MatchRequests")
            val matchRequest = hashMapOf(
                "sender" to auth.currentUser!!.email!!,
                "receiver" to user.email,
                "requestStatus" to 0
            )
            matchRequestsCollection.add(matchRequest).addOnSuccessListener {
                //send notification to receiver
                val notificationTitle = getString(R.string.have_new_match_request)
                val notificationMessage = getString(R.string.have_new_match_request_message)
                val receiverToken = user.fcmToken

                if(receiverToken != null){
                    val jsonBody = JSONObject()
                    jsonBody.put("to", receiverToken)

                    val notification = JSONObject()
                    notification.put("title", notificationTitle)
                    notification.put("body", notificationMessage)
                    notification.put("notificationType", "requestSent")

                    jsonBody.put("notification", notification)

                    val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("https://fcm.googleapis.com/fcm/send")
                        .addHeader("Authorization", "key=$SERVER_KEY")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient()


                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.d("Notification", "onFailure: $e")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            Log.d("Notification", "onResponse: $response")
                        }
                    })
                }

                Toast.makeText(context, R.string.match_request_sent, Toast.LENGTH_LONG).show()
                getBack()
            }.addOnFailureListener {
                Toast.makeText(context, R.string.match_request_failed, Toast.LENGTH_LONG).show()
            }
        }
        alertDialog.setNegativeButton(R.string.no) { _, _ -> }
        alertDialog.show()
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

interface UserDataCallback {
    fun onUserDataLoaded(user: User)
}
