package com.erensekkeli.roomieconnect.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firestore= Firebase.firestore

        val intent = intent
        val email = intent.getBooleanExtra("verification", false)

        if(email) {
            binding.sendVerificationEmailButton.visibility = View.VISIBLE
            val currentUser = auth.currentUser
            currentUser?.sendEmailVerification()?.addOnSuccessListener {
                Toast.makeText(this, R.string.verification_email_sent, Toast.LENGTH_LONG).show()
            }?.addOnFailureListener {
                Toast.makeText(this, R.string.verification_email_not_sent, Toast.LENGTH_LONG).show()
            }
        }

        //text underline
        binding.haventAccount.paint.isUnderlineText = true

        sharedPreferences = this.getSharedPreferences("com.erensekkeli.roomieconnect", MODE_PRIVATE)
        val remindMe = sharedPreferences.getBoolean("remindMe", false)

        if(remindMe) {
            val currentUser = auth.currentUser
            if(currentUser != null && currentUser.isEmailVerified) {
                val intent = Intent(this@MainActivity, FeedActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    }

    fun goSignUp(view: View) {
        val intent = Intent(this@MainActivity, SignUpActivity::class.java)
        startActivity(intent)
    }

    fun sendMailAgain(view: View) {
        val currentUser = auth.currentUser
        binding.sendVerificationEmailButton.visibility = View.INVISIBLE
        currentUser?.sendEmailVerification()?.addOnSuccessListener {
            Toast.makeText(this, R.string.verification_email_sent, Toast.LENGTH_LONG).show()
        }?.addOnFailureListener {
            Toast.makeText(this, R.string.verification_email_not_sent, Toast.LENGTH_LONG).show()
        }
    }

    private fun getProcessAnimation() {
        binding.progressBarLogin.visibility = View.VISIBLE
        binding.signInButton.visibility = View.INVISIBLE
        val overlayView = View(this)
        overlayView.setBackgroundColor(Color.parseColor("#99000000"))
        overlayView.alpha = 0.5f
        overlayView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.root.addView(overlayView)
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun removeProcessAnimation() {
        binding.progressBarLogin.visibility = View.INVISIBLE
        binding.signInButton.visibility = View.VISIBLE
        binding.root.removeViewAt(binding.root.childCount - 1)
    }

    fun signIn(view: View) {
        hideKeyboard()
        val email = binding.emailInputForLogin.text.toString()
        val password = binding.passInputForLogin.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.email_or_password_empty, Toast.LENGTH_SHORT).show()
            return
        }

        getProcessAnimation()
        val currentUser = auth.currentUser

        if(currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (currentUser.isEmailVerified) {
                        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                            removeProcessAnimation()
                            val intent = Intent(this@MainActivity, FeedActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            sharedPreferences.edit()
                                ?.putBoolean("remindMe", binding.remindMeCheckBox.isChecked)
                                ?.apply()
                            firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get()
                                .addOnSuccessListener {
                                    val document = it.documents[0]
                                    sharedPreferences.edit().putInt("status", document.getLong("status")!!.toInt()).apply()
                                    startActivity(intent)
                                    finish()
                                }.addOnFailureListener {
                                    Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                                    removeProcessAnimation()
                                }
                        }.addOnFailureListener {
                            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show()
                            removeProcessAnimation()
                        }
                    } else {
                        Toast.makeText(this, R.string.email_not_verified, Toast.LENGTH_SHORT).show()
                        removeProcessAnimation()
                    }
                } else {
                    Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                    removeProcessAnimation()
                }
            }
        } else {
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                removeProcessAnimation()
                val intent = Intent(this@MainActivity, FeedActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                sharedPreferences?.edit()
                    ?.putBoolean("remindMe", binding.remindMeCheckBox.isChecked)
                    ?.apply()
                firestore.collection("UserData").whereEqualTo("email", auth.currentUser!!.email).get()
                    .addOnSuccessListener {
                        val document = it.documents[0]
                        sharedPreferences.edit().putInt("status", document.getLong("status")!!.toInt()).apply()
                    }.addOnFailureListener {
                        Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                    }
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show()
                removeProcessAnimation()
            }
        }
    }
}