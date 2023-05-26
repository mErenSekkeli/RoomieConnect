package com.erensekkeli.roomieconnect.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.activities.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

const val CHANNEL_ID = "notificationChannel"
const val CHANNEL_NAME = "RoomieConnect"
class FirebaseNotificationService: FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.notification != null) {
            val type = remoteMessage.data["notificationType"]
            generateNotification(remoteMessage.notification!!.title!!, remoteMessage.notification!!.body!!, type)
        }
    }

    @SuppressLint("RemoteViewLayout")
    fun getRemoteView(title: String, body: String): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.notification_template)
        remoteViews.setTextViewText(R.id.notificationTitle, title)
        remoteViews.setTextViewText(R.id.notificationBody, body)
        remoteViews.setImageViewResource(R.id.appIcon, R.drawable.app_icon)
        return remoteViews
    }

    private fun generateNotification(title: String, body: String, type: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if(type != null) {
            if(type == "requestSent") {
                intent.putExtra("requestSent", true)
            }

        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.app_icon)
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(getRemoteView(title, body))

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, builder.build())

    }

}