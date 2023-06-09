package com.erensekkeli.roomieconnect.fragments


import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.MainActivity
import com.erensekkeli.roomieconnect.databinding.FragmentProfileSettingsBinding
import com.erensekkeli.roomieconnect.guitools.PasswordChangeDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID


class ProfileSettingsFragment : Fragment(), PasswordChangeDialog.PasswordChangeDialogListener {

    private lateinit var binding: FragmentProfileSettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var selectedImage: Uri? = null
    private lateinit var requestedPermission: String
    private var profilePicture: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage

        registerLauncher()
        getData()

        binding.saveData.setOnClickListener {
            saveData(view)
        }

        binding.profileImage.setOnClickListener {
            changeProfileImage(view)
        }
        binding.changePasswordButton.setOnClickListener {
            changePassword(view)
        }

        binding.backBtn.setOnClickListener {
            getBack()
        }
        binding.logoutButton.setOnClickListener {
            logoutClickListener(view)
        }
    }

    private fun getProcessAnimation() {
        binding.progressContainer.visibility = View.VISIBLE

    }

    private fun removeProcessAnimation() {
        binding.progressContainer.visibility = View.INVISIBLE
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                selectedImage = if (result.data?.data != null) {
                    result.data?.data
                } else {
                    imageUri
                }
                if (selectedImage != null && profilePicture != null ) {
                    binding.profileImage.setImageURI(selectedImage)
                    val reference = storage.reference
                    val uuid = profilePicture!!.substringAfterLast("/").split("?")[0].split("user_profile_images")[1].replaceFirst("%2F","/")
                    val imageRef = reference.child("user_profile_images$uuid")
                    val newUUID = UUID.randomUUID()
                    val newImageRef = reference.child("user_profile_images/${newUUID.toString()}.jpg")
                    imageRef.delete().addOnSuccessListener {
                        newImageRef.putFile(selectedImage!!).addOnSuccessListener {
                            newImageRef.downloadUrl.addOnSuccessListener { uri ->
                                firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email!!).get()
                                    .addOnSuccessListener { documents ->
                                        val document = documents.documents[0]
                                        val docId = document.id

                                        val data: HashMap<String, Any> = hashMapOf(
                                            "profileImage" to uri.toString()
                                        )
                                        firestore.collection("UserData").document(docId).update(data)
                                            .addOnSuccessListener {
                                                profilePicture = uri.toString()
                                                Toast.makeText(context,
                                                    R.string.profile_image_updated, Toast.LENGTH_SHORT).show()
                                            }.addOnFailureListener { exception ->
                                                Toast.makeText(context,
                                                    R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                                            }

                                    }.addOnFailureListener { exception ->
                                        Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                                    }

                            }.addOnFailureListener {
                                Toast.makeText(context, R.string.profile_image_update_failed, Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, R.string.profile_image_update_failed, Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                }else if(selectedImage != null && profilePicture == null) {
                    //add new profile picture
                    binding.profileImage.setImageURI(selectedImage)
                    val reference = storage.reference
                    val newUUID = UUID.randomUUID()
                    val newImageRef = reference.child("user_profile_images/${newUUID.toString()}.jpg")
                    newImageRef.putFile(selectedImage!!).addOnSuccessListener {
                        newImageRef.downloadUrl.addOnSuccessListener { uri ->
                            firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email!!).get()
                                .addOnSuccessListener { documents ->
                                    val document = documents.documents[0]
                                    val docId = document.id

                                    val data: HashMap<String, Any> = hashMapOf(
                                        "profileImage" to uri.toString()
                                    )
                                    firestore.collection("UserData").document(docId).update(data)
                                        .addOnSuccessListener {
                                            profilePicture = uri.toString()
                                            Toast.makeText(context,
                                                R.string.profile_image_updated, Toast.LENGTH_SHORT).show()
                                        }.addOnFailureListener {
                                            Toast.makeText(context,
                                                R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                                        }

                                }.addOnFailureListener {
                                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                                }

                        }.addOnFailureListener {
                            Toast.makeText(context, R.string.profile_image_update_failed, Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, R.string.profile_image_update_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(context, R.string.failed_to_get_image, Toast.LENGTH_SHORT).show()
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if(result) {
                when(requestedPermission) {
                    android.Manifest.permission.CAMERA -> {
                        val values = ContentValues()
                        values.put(MediaStore.Images.Media.TITLE, "New Picture")
                        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                        imageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                        if (imageUri != null) {
                            val intentToCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            intentToCamera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                            intentToCamera.putExtra("isCamera", true)
                            activityResultLauncher.launch(intentToCamera)
                        } else {
                            Toast.makeText(context, R.string.failed_to_get_image, Toast.LENGTH_SHORT).show()
                        }
                    }
                    android.Manifest.permission.READ_EXTERNAL_STORAGE -> {
                        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        activityResultLauncher.launch(intentToGallery)
                    }
                }
            } else {
                Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getData() {
        getProcessAnimation()
        val usersCollection = firestore.collection("UserData")

        usersCollection.whereEqualTo("email", auth.currentUser!!.email!!).get().addOnSuccessListener { documents ->
            if(documents != null && !documents.isEmpty) {
                val document = documents.documents[0]
                val name = document.get("name")?.toString() ?: "-"
                val surname = document.get("surname")?.toString() ?: "-"
                val department = document.get("department")?.toString() ?: "-"
                val status = document.get("status")?.toString()?.toInt() ?: 0
                val gradeYear = document.get("gradeYear")?.toString()?.toInt() ?: 0
                val homeTime = document.get("homeTime")?.toString()?.toInt() ?: 0
                val campusDistance = document.get("campusDistance")?.toString()?.toInt() ?: 0
                profilePicture = document.get("profileImage")?.toString()
                val contactPhone = document.get("contactPhone")?.toString() ?: "-"
                val contactMail = document.get("contactMail")?.toString() ?: "-"

                binding.profileNameSurname.text = "$name $surname"
                binding.nameField.setText(name)
                binding.surnameField.setText(surname)
                val imageUri: Uri? = profilePicture?.toUri()
                if(imageUri == null || imageUri.toString() == "") {
                    binding.profileImage.setImageResource(R.drawable.app_icon)
                } else {
                    Glide.with(this@ProfileSettingsFragment).load(imageUri).into(binding.profileImage)
                }
                binding.contactPhoneField.setText(contactPhone)
                binding.contactMailField.setText(contactMail)
                binding.departmantField.setText(department)
                binding.statusSpinner.setSelection(status)
                binding.gradeYearField.setText(gradeYear.toString())
                binding.homeTimeField.setText(homeTime.toString())
            }
            removeProcessAnimation()
        }.addOnFailureListener { exception ->
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            removeProcessAnimation()
        }

    }

    private fun permissionType(permissionType: String, view: View) {
        if (ContextCompat.checkSelfPermission(
                view.context,
                permissionType
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissionType)) {
                Snackbar.make(view, R.string.give_external_permission, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.give_permission) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            requestedPermission = permissionType
                            permissionLauncher.launch(permissionType)
                        }
                    }.show()
            } else {
                requestedPermission = permissionType
                permissionLauncher.launch(permissionType)
            }
        }else {
            if(permissionType == android.Manifest.permission.CAMERA) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                imageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                if (imageUri != null) {
                    val intentToCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intentToCamera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    intentToCamera.putExtra("isCamera", true)
                    activityResultLauncher.launch(intentToCamera)
                } else {
                    Toast.makeText(context, R.string.failed_to_get_image, Toast.LENGTH_SHORT).show()
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    fun changeProfileImage(view: View) {
        val alert = AlertDialog.Builder(requireContext())
        alert.setTitle(R.string.choose_image_from_gallery)
        alert.setMessage(R.string.choose_image_from)
        alert.setPositiveButton(R.string.gallery) { _, _ ->
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionType(android.Manifest.permission.READ_MEDIA_IMAGES, view)
            } else {
                permissionType(android.Manifest.permission.READ_EXTERNAL_STORAGE, view)
            }
        }
        alert.setNegativeButton(R.string.camera) { _, _ ->
            permissionType(android.Manifest.permission.CAMERA, view)
        }
        alert.show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    fun saveData(view: View) {
        hideKeyboard()
        val name = binding.nameField.text.toString()
        val surname = binding.surnameField.text.toString()
        val department = binding.departmantField.text.toString()
        val statusIndex = resources.getStringArray(R.array.student_status).indexOf(binding.statusSpinner.selectedItem.toString())
        val status = statusIndex
        val gradeYear = binding.gradeYearField.text.toString().toInt()
        val homeTime = binding.homeTimeField.text.toString().toInt()
        val contactPhone = binding.contactPhoneField.text.toString()
        val contactMail = binding.contactMailField.text.toString()

        if(name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(context, R.string.fill_requried_fields, Toast.LENGTH_LONG).show()
        } else {
            getProcessAnimation()
            val usersCollection = firestore.collection("UserData")

            usersCollection.whereEqualTo("email", auth.currentUser!!.email!!).get().addOnSuccessListener { documents ->
                if(documents != null && !documents.isEmpty) {
                    val document = documents.documents[0]
                    val documentId = document.id
                    val user: HashMap<String, Any> = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "department" to department,
                        "status" to status,
                        "gradeYear" to gradeYear,
                        "homeTime" to homeTime,
                        "contactPhone" to contactPhone,
                        "contactMail" to contactMail
                    )

                    usersCollection.document(documentId).update(user).addOnSuccessListener {
                        Toast.makeText(context, R.string.user_data_saved, Toast.LENGTH_LONG).show()
                        removeProcessAnimation()
                        getBack()
                    }.addOnFailureListener {
                        Toast.makeText(context, R.string.user_data_save_failed, Toast.LENGTH_LONG).show()
                        removeProcessAnimation()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, R.string.user_data_save_failed, Toast.LENGTH_LONG).show()
                removeProcessAnimation()
            }
        }
    }

    private fun getBack() {
        hideKeyboard()
        val fragmentManager = activity?.supportFragmentManager
        fragmentManager?.popBackStack()
    }

    private fun changePassword(view: View) {
        val dialog = PasswordChangeDialog(view.context, this)
        dialog.show()
    }

    fun logoutClickListener(view: View) {
        //open confirmation dialog
        val builder = AlertDialog.Builder(view.context)
        builder.setTitle(R.string.exit_app)
        builder.setMessage(R.string.exit_app_message)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            auth.signOut()
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.no) { _, _ ->

        }
        builder.show()
    }

    override fun onDialogOkButtonClicked(input: String, inputAgain: String) {
        if(input == inputAgain) {
            val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+\$).{8,}\$")
            if(!passwordRegex.matches(input)) {
                Toast.makeText(context, R.string.password_format_error, Toast.LENGTH_SHORT).show()
                return
            }

            getProcessAnimation()
            auth.currentUser!!.updatePassword(input).addOnSuccessListener {
                Toast.makeText(context, R.string.password_changed, Toast.LENGTH_LONG).show()
                removeProcessAnimation()
            }.addOnFailureListener {
                Toast.makeText(context, R.string.password_change_failed, Toast.LENGTH_LONG).show()
                removeProcessAnimation()
            }
        } else {
            Toast.makeText(context, R.string.passwords_not_match, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDialogCancelButtonClicked() {
        Toast.makeText(context, R.string.password_change_canceled, Toast.LENGTH_LONG).show()
    }

}