package com.example.subscriptionmanager.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: String,
    val name: String,
    val role: String,
    val months_used: Int,
    val auth_id: String? = null,
    val fcm_token: String? = null,
    val created_at: String? = null
)

@Serializable
data class Payment(
    val id: String? = null,
    val member_id: String,
    val amount: Double,
    val payment_date: String? = null,
    val payment_for_month: String? = null,
    val created_at: String? = null
)

@Serializable
data class NewMember(
    val name: String,
    val role: String = "member",
    val months_used: Int = 0
)

@Serializable
data class AppNotification(
    val id: String? = null,
    val member_id: String,
    val message: String,
    val is_read: Boolean = false,
    val created_at: String? = null
)

class SubscriptionRepository {
    private val supabase = SupabaseApi.client

    suspend fun getMembers(): List<Member> = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].select().decodeList<Member>()
    }

    suspend fun getMemberByAuthId(authId: String): Member? = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].select {
            filter { eq("auth_id", authId) }
        }.decodeList<Member>().firstOrNull()
    }

    suspend fun getUnclaimedMembers(): List<Member> = withContext(Dispatchers.IO) {
        // Only return members that haven't been linked to an account yet
        getMembers().filter { it.auth_id.isNullOrBlank() }
    }

    suspend fun updateFcmToken(memberId: String, token: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].update(mapOf("fcm_token" to token)) {
            filter { eq("id", memberId) }
        }
    }

    suspend fun claimProfile(memberId: String, authId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].update(mapOf("auth_id" to authId)) {
            filter { eq("id", memberId) }
        }
    }

    suspend fun getPaymentsForUser(memberId: String): List<Payment> = withContext(Dispatchers.IO) {
        supabase.postgrest["payments"].select {
            filter { eq("member_id", memberId) }
        }.decodeList<Payment>()
    }

    suspend fun addPayment(memberId: String, amount: Double, paymentDate: String, paymentForMonth: String) = withContext(Dispatchers.IO) {
        val payment = Payment(member_id = memberId, amount = amount, payment_date = paymentDate, payment_for_month = paymentForMonth)
        supabase.postgrest["payments"].insert(payment)
    }

    suspend fun addMember(name: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].insert(NewMember(name = name))
    }

    suspend fun deleteMember(memberId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].delete {
            filter { eq("id", memberId) }
        }
    }

    suspend fun incrementMonthsUsedForEveryone() = withContext(Dispatchers.IO) {
        val members = getMembers()
        for (member in members) {
            supabase.postgrest["members"].update(mapOf("months_used" to member.months_used + 1)) {
                filter { eq("id", member.id) }
            }
        }
    }

    suspend fun deletePayment(paymentId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["payments"].delete {
            filter { eq("id", paymentId) }
        }
    }

    suspend fun sendPing(memberId: String, message: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["notifications"].insert(
            AppNotification(member_id = memberId, message = message)
        )
    }

    suspend fun getUnreadNotifications(memberId: String): List<AppNotification> = withContext(Dispatchers.IO) {
        supabase.postgrest["notifications"].select {
            filter {
                eq("member_id", memberId)
                eq("is_read", false)
            }
        }.decodeList<AppNotification>()
    }

    suspend fun markNotificationsRead(memberId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter { eq("member_id", memberId) }
        }
    }
}
