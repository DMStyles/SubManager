package com.example.subscriptionmanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subscriptionmanager.MainActivity
import com.example.subscriptionmanager.data.SubscriptionRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return@withContext Result.success()
                }
            }

            // Get shared preferences for settings
            val sharedPrefs = appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
            val daysBeforeToRemind = sharedPrefs.getInt("days_before_reminder", 4)

            if (!notificationsEnabled) return@withContext Result.success()

            // Calculate days until the 8th
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_MONTH)
            val targetDay = 8
            
            val daysUntilDue = targetDay - today

            // If it's between 1 and `daysBeforeToRemind` days until the 8th
            if (daysUntilDue in 1..daysBeforeToRemind) {
                // Check user status
                val repository = SubscriptionRepository()
                val supabase = com.example.subscriptionmanager.data.SupabaseApi.client
                val authUser = supabase.auth.currentUserOrNull() ?: return@withContext Result.success()
                
                val member = repository.getMemberByAuthId(authUser.id) ?: return@withContext Result.success()
                val payments = repository.getPaymentsForUser(member.id)
                val totalPaidAmount = payments.sumOf { it.amount }
                val totalMonthsPaid = (totalPaidAmount / 211.0).toInt()

                val cal = java.util.Calendar.getInstance()
                val currentYear = cal.get(java.util.Calendar.YEAR)
                val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
                var dynamicMonthsUsed = (currentYear - 2026) * 12 + (currentMonth - 5) + 1
                if (dynamicMonthsUsed < 1) dynamicMonthsUsed = 1

                val remainingBalance = totalMonthsPaid - dynamicMonthsUsed
                
                // Remind based on payment status
                if (remainingBalance <= 0) {
                    sendNotification(daysUntilDue, hasPaid = false)
                } else {
                    sendNotification(daysUntilDue, hasPaid = true)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(daysUntilDue: Int, hasPaid: Boolean) {
        val channelId = "payment_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Payment Reminders"
            val descriptionText = "Reminders for subscription payments"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val title = if (hasPaid) "Payment Update" else "Payment Due Soon"
        val text = if (hasPaid) {
            "Great news! Your subscription is paid ahead for the upcoming 8th."
        } else {
            "Your subscription payment of Rs. 212 is due in $daysUntilDue day(s)."
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(appContext)) {
            notify(1001, builder.build())
        }
    }
}
