package com.example.subscriptionmanager.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.subscriptionmanager.data.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // We handle saving the token when the user logs in, but we can also handle it here if needed.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received: ${remoteMessage.data}")
        // Since we are sending Notification messages from the Admin SDK, 
        // the Android system will automatically display them in the system tray when the app is in the background.
        // We only need to handle them here if the app is in the foreground.
    }
}
