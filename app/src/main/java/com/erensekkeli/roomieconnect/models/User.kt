package com.erensekkeli.roomieconnect.models

import java.io.Serializable

class User(email: String?, name: String, surname: String, fcmToken: String?, contactMail: String?, contactPhone: String?, department: String?, status: Int?, profileImage: String?, campusDistance: Int?,
            gradeYear: Int?, homeTime: Int?): Serializable {

    val email: String? = email
    val name: String = name
    val surname: String = surname
    val fcmToken: String? = fcmToken
    val contactMail: String? = contactMail
    val contactPhone: String? = contactPhone
    val department: String? = department
    val status: Int? = status
    val campusDistance: Int? = campusDistance
    val gradeYear: Int? = gradeYear
    val homeTime: Int? = homeTime
    val profileImage: String? = profileImage



}