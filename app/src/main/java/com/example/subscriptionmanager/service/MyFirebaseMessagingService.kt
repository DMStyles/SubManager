package com.example.subscriptionmanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.subscriptionmanager.MainActivity
import com.example.subscriptionmanager.R
import com.example.subscriptionmanager.data.SubscriptionRepository
import com.example.subscriptionmanager.data.SupabaseApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        val authUser = SupabaseApi.client.auth.currentUserOrNull()
        if (authUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = SubscriptionRepository()
                    val member = repo.getMemberByAuthId(authUser.id)
                    if (member != null) {
                        repo.updateFcmToken(member.id, token)
                        Log.d("FCM", "Updated token in Supabase for ${member.name}")
                    }
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to update token in Supabase", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received: ${remoteMessage.data}, notification: ${remoteMessage.notification?.body}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Spotify Payment Reminder"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "You have a new payment reminder."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "payment_reminders"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Spotify payment reminders"
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e("FCM", "Notification permission not granted", e)
        }
    }
}
