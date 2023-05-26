package com.erensekkeli.roomieconnect.adapters

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.FeedActivity
import com.erensekkeli.roomieconnect.databinding.RequestItemBinding
import com.erensekkeli.roomieconnect.fragments.ProfileDetailFragment
import com.erensekkeli.roomieconnect.fragments.SERVER_KEY
import com.erensekkeli.roomieconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class RequestListAdapter(private val requestedUserList: ArrayList<User>): RecyclerView.Adapter<RequestListAdapter.RequestViewHolder>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    class RequestViewHolder(val binding: RequestItemBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        auth = Firebase.auth
        firestore = Firebase.firestore
        val binding = RequestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return requestedUserList.size
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = requestedUserList[position]
        holder.binding.nameSurnameTitle.text = user.name + " " + user.surname
        holder.binding.contactMail.text = user.contactMail ?: "-"
        holder.binding.contactPhone.text = user.contactPhone ?: "-"
        holder.binding.gradeYear.text = user.gradeYear.toString() ?: "-"
        val imageUri: Uri? = user.profileImage?.toUri()
        if(imageUri != null) {
            Glide.with(holder.itemView.context).load(imageUri).into(holder.binding.profileImage)
        }else {
            Glide.with(holder.itemView.context).load(R.drawable.app_icon).into(holder.binding.profileImage)
        }
        holder.itemView.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("user", requestedUserList[position])
            val fragment = ProfileDetailFragment()
            fragment.arguments = bundle
            val transaction = (holder.itemView.context as FeedActivity).supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.anim.enter_right_to_left,
                R.anim.exit_right_to_left,
                R.anim.enter_left_to_right,
                R.anim.exit_left_to_right
            )
            transaction.replace(R.id.feedContainerFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        holder.binding.acceptRequest.setOnClickListener {
            val alertDialog = AlertDialog.Builder(holder.itemView.context)
            alertDialog.setTitle(R.string.accept_request)
            alertDialog.setMessage(R.string.accept_request_message)
            alertDialog.setPositiveButton(R.string.yes) { _, _ ->
                acceptRequest(user, holder)
                requestedUserList.remove(user)
                notifyDataSetChanged()
                if(requestedUserList.size == 0) {
                    (holder.itemView.context as FeedActivity).supportFragmentManager.popBackStack()
                }
                Toast.makeText(holder.itemView.context, R.string.request_accepted, Toast.LENGTH_SHORT).show()
            }
            alertDialog.setNegativeButton(R.string.no) { _, _ ->
                Toast.makeText(holder.itemView.context, R.string.request_not_accepted, Toast.LENGTH_SHORT).show()
            }
            alertDialog.show()
        }

        holder.binding.rejectRequestBtn.setOnClickListener {
            val alertDialog = AlertDialog.Builder(holder.itemView.context)
            alertDialog.setTitle(R.string.reject_request)
            alertDialog.setMessage(R.string.reject_request_message)
            alertDialog.setPositiveButton(R.string.yes) { _, _ ->
                rejectRequest(user, holder)
                requestedUserList.remove(user)
                notifyDataSetChanged()
                if(requestedUserList.size == 0) {
                    (holder.itemView.context as FeedActivity).supportFragmentManager.popBackStack()
                }
                Toast.makeText(holder.itemView.context, R.string.request_rejected, Toast.LENGTH_SHORT).show()
            }
            alertDialog.setNegativeButton(R.string.no) { _, _ ->
                Toast.makeText(holder.itemView.context, R.string.request_not_rejected, Toast.LENGTH_SHORT).show()
            }
            alertDialog.show()
        }

    }

    private fun acceptRequest(user: User, holder: RequestViewHolder) {
        val receiverEmail = auth.currentUser!!.email
        val senderEmail = user.email

        firestore.collection("MatchRequests").whereEqualTo("receiver", receiverEmail).whereEqualTo("sender", senderEmail).get()
            .addOnSuccessListener {
                val docId = it.documents[0].id
                firestore.collection("MatchRequests").document(docId).update("requestStatus", 1)

                firestore.collection("UserData").whereEqualTo("email", receiverEmail).get()
                    .addOnSuccessListener {
                        val docId = it.documents[0].id
                        firestore.collection("UserData").document(docId).update("status", 0)
                        sendAcceptedNotification(user, holder)
                    }

                firestore.collection("UserData").whereEqualTo("email", senderEmail).get()
                    .addOnSuccessListener {
                        val docId = it.documents[0].id
                        firestore.collection("UserData").document(docId).update("status", 0)
                    }
            }
    }

    private fun rejectRequest(user: User, holder: RequestViewHolder) {
        val receiverEmail = auth.currentUser!!.email
        val senderEmail = user.email

        firestore.collection("MatchRequests").whereEqualTo("receiver", receiverEmail).whereEqualTo("sender", senderEmail).get()
            .addOnSuccessListener {
                val docId = it.documents[0].id
                firestore.collection("MatchRequests").document(docId).update("requestStatus", 2)
                sendRejectedNotification(user, holder)
            }
    }

    private fun sendAcceptedNotification(user: User, holder: RequestViewHolder) {

        firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get()
            .addOnSuccessListener {
                val document = it.documents[0]
                val nameSurname = document.getString("name") + " " + document.getString("surname")
                val notificationTitle =
                    holder.itemView.context.getString(R.string.match_request_accepted)
                val notificationMessage =
                    "$nameSurname " + holder.itemView.context.getString(R.string.match_request_accepted_message)
                val receiverToken = user.fcmToken

                if (receiverToken != null) {
                    val jsonBody = JSONObject()
                    jsonBody.put("to", receiverToken)

                    val notification = JSONObject()
                    notification.put("title", notificationTitle)
                    notification.put("body", notificationMessage)
                    notification.put("notificationType", "requestSent")

                    jsonBody.put("notification", notification)

                    val requestBody = jsonBody.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

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
            }
    }

    private fun sendRejectedNotification(user: User, holder: RequestViewHolder) {

        firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get()
            .addOnSuccessListener {
                val document = it.documents[0]
                val nameSurname = document.getString("name") + " " + document.getString("surname")
                val notificationTitle = holder.itemView.context.getString(R.string.match_request_rejected)
                val notificationMessage = "$nameSurname " + holder.itemView.context.getString(R.string.match_request_rejected_message)
                val receiverToken = user.fcmToken

                if (receiverToken != null) {
                    val jsonBody = JSONObject()
                    jsonBody.put("to", receiverToken)

                    val notification = JSONObject()
                    notification.put("title", notificationTitle)
                    notification.put("body", notificationMessage)
                    notification.put("notificationType", "requestSent")

                    jsonBody.put("notification", notification)

                    val requestBody = jsonBody.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

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
            }
    }

}