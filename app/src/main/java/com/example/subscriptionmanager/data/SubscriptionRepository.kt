package com.example.subscriptionmanager.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────
// Existing models (unchanged)
// ─────────────────────────────────────────────

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
    val subscription_id: String? = null,
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
    val subscription_id: String? = null,
    val message: String,
    val is_read: Boolean = false,
    val created_at: String? = null
)

// ─────────────────────────────────────────────
// v2.0 Models
// ─────────────────────────────────────────────

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val description: String? = null,
    val monthly_cost: Double = 211.0,
    val billing_day: Int = 8,
    val start_date: String = "2026-05-01",
    val max_members: Int = 6,
    val admin_id: String? = null,
    val invite_code: String = "",
    val invite_expires_at: String? = null,
    val created_at: String? = null
)

@Serializable
data class NewSubscription(
    val name: String,
    val description: String? = null,
    val monthly_cost: Double,
    val billing_day: Int,
    val start_date: String,
    val max_members: Int,
    val admin_id: String,
    val invite_code: String,
    val invite_expires_at: String
)

@Serializable
data class SubscriptionMember(
    val id: String? = null,
    val subscription_id: String,
    val member_id: String,
    val role: String = "member",
    val joined_at: String? = null
)

@Serializable
data class UpdateSubscriptionPayload(
    val name: String,
    val description: String?,
    val monthly_cost: Double,
    val billing_day: Int,
    val start_date: String,
    val max_members: Int
)

@Serializable
data class UpdateInviteCodePayload(
    val invite_code: String,
    val invite_expires_at: String
)

@Serializable
data class UpdateFcmPayload(val fcm_token: String)

@Serializable
data class UpdateAuthIdPayload(val auth_id: String)

@Serializable
data class UpdateMonthsPayload(val months_used: Int)

@Serializable
data class UpdateIsReadPayload(val is_read: Boolean)

// ─────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────

class SubscriptionRepository {
    private val supabase = SupabaseApi.client

    // ── Members ──────────────────────────────

    suspend fun getMembers(): List<Member> = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].select().decodeList<Member>()
    }

    suspend fun getMemberByAuthId(authId: String): Member? = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].select {
            filter { eq("auth_id", authId) }
        }.decodeList<Member>().firstOrNull()
    }

    suspend fun getUnclaimedMembers(): List<Member> = withContext(Dispatchers.IO) {
        getMembers().filter { it.auth_id.isNullOrBlank() }
    }

    suspend fun updateFcmToken(memberId: String, token: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].update(UpdateFcmPayload(fcm_token = token)) {
            filter { eq("id", memberId) }
        }
    }

    suspend fun claimProfile(memberId: String, authId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["members"].update(UpdateAuthIdPayload(auth_id = authId)) {
            filter { eq("id", memberId) }
        }
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
            supabase.postgrest["members"].update(UpdateMonthsPayload(months_used = member.months_used + 1)) {
                filter { eq("id", member.id) }
            }
        }
    }

    // ── Payments ──────────────────────────────

    suspend fun getPaymentsForUser(memberId: String, subscriptionId: String? = null): List<Payment> =
        withContext(Dispatchers.IO) {
            supabase.postgrest["payments"].select {
                filter {
                    eq("member_id", memberId)
                    if (subscriptionId != null) eq("subscription_id", subscriptionId)
                }
            }.decodeList<Payment>()
        }

    suspend fun addPayment(
        memberId: String,
        amount: Double,
        paymentDate: String,
        paymentForMonth: String,
        subscriptionId: String? = null
    ) = withContext(Dispatchers.IO) {
        val payment = Payment(
            member_id = memberId,
            amount = amount,
            payment_date = paymentDate,
            payment_for_month = paymentForMonth,
            subscription_id = subscriptionId
        )
        supabase.postgrest["payments"].insert(payment)
    }

    suspend fun deletePayment(paymentId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["payments"].delete {
            filter { eq("id", paymentId) }
        }
    }

    // ── Notifications ─────────────────────────

    suspend fun sendPing(memberId: String, subscriptionId: String, message: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["notifications"].insert(
            AppNotification(member_id = memberId, subscription_id = subscriptionId, message = message)
        )
    }

    suspend fun getUnreadNotifications(memberId: String, subscriptionId: String): List<AppNotification> =
        withContext(Dispatchers.IO) {
            supabase.postgrest["notifications"].select {
                filter {
                    eq("member_id", memberId)
                    eq("subscription_id", subscriptionId)
                    eq("is_read", false)
                }
            }.decodeList<AppNotification>()
        }

    suspend fun markNotificationsRead(memberId: String, subscriptionId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["notifications"].update(UpdateIsReadPayload(is_read = true)) {
            filter {
                eq("member_id", memberId)
                eq("subscription_id", subscriptionId)
            }
        }
    }

    // ── Subscriptions (v2.0) ──────────────────

    suspend fun getSubscriptionsForMember(memberId: String): List<Subscription> =
        withContext(Dispatchers.IO) {
            // Get subscription IDs for this member
            val memberLinks = supabase.postgrest["subscription_members"].select {
                filter { eq("member_id", memberId) }
            }.decodeList<SubscriptionMember>()

            if (memberLinks.isEmpty()) return@withContext emptyList()

            // Fetch each subscription
            val ids = memberLinks.map { it.subscription_id }
            supabase.postgrest["subscriptions"].select {
                filter { isIn("id", ids) }
            }.decodeList<Subscription>()
        }

    suspend fun getSubscriptionById(subscriptionId: String): Subscription? =
        withContext(Dispatchers.IO) {
            supabase.postgrest["subscriptions"].select {
                filter { eq("id", subscriptionId) }
            }.decodeList<Subscription>().firstOrNull()
        }

    suspend fun getMembersForSubscription(subscriptionId: String): List<Member> =
        withContext(Dispatchers.IO) {
            val links = supabase.postgrest["subscription_members"].select {
                filter { eq("subscription_id", subscriptionId) }
            }.decodeList<SubscriptionMember>()

            if (links.isEmpty()) return@withContext emptyList()

            val memberIds = links.map { it.member_id }
            supabase.postgrest["members"].select {
                filter { isIn("id", memberIds) }
            }.decodeList<Member>()
        }

    suspend fun getRoleInSubscription(subscriptionId: String, memberId: String): String =
        withContext(Dispatchers.IO) {
            supabase.postgrest["subscription_members"].select {
                filter {
                    eq("subscription_id", subscriptionId)
                    eq("member_id", memberId)
                }
            }.decodeList<SubscriptionMember>().firstOrNull()?.role ?: "member"
        }

    suspend fun createSubscription(
        name: String,
        description: String?,
        monthlyCost: Double,
        billingDay: Int,
        startDate: String,
        maxMembers: Int,
        adminId: String,
        creatorMemberId: String
    ): Subscription = withContext(Dispatchers.IO) {
        // Generate a unique invite code
        val inviteCode = generateInviteCode()
        val expiresAt = getExpiryTimestamp()

        val newSub = NewSubscription(
            name = name,
            description = description,
            monthly_cost = monthlyCost,
            billing_day = billingDay,
            start_date = startDate,
            max_members = maxMembers,
            admin_id = adminId,
            invite_code = inviteCode,
            invite_expires_at = expiresAt
        )
        val created = supabase.postgrest["subscriptions"].insert(newSub) {
            select()
        }.decodeList<Subscription>().first()

        // Add creator as admin member
        supabase.postgrest["subscription_members"].insert(
            SubscriptionMember(
                subscription_id = created.id,
                member_id = creatorMemberId,
                role = "admin"
            )
        )
        created
    }

    suspend fun updateSubscription(
        subscriptionId: String,
        name: String,
        description: String?,
        monthlyCost: Double,
        billingDay: Int,
        startDate: String,
        maxMembers: Int
    ) = withContext(Dispatchers.IO) {
        supabase.postgrest["subscriptions"].update(
            UpdateSubscriptionPayload(
                name = name,
                description = description,
                monthly_cost = monthlyCost,
                billing_day = billingDay,
                start_date = startDate,
                max_members = maxMembers
            )
        ) {
            filter { eq("id", subscriptionId) }
        }
    }

    suspend fun refreshInviteCode(subscriptionId: String): String = withContext(Dispatchers.IO) {
        val newCode = generateInviteCode()
        val expiresAt = getExpiryTimestamp()
        supabase.postgrest["subscriptions"].update(
            UpdateInviteCodePayload(
                invite_code = newCode,
                invite_expires_at = expiresAt
            )
        ) {
            filter { eq("id", subscriptionId) }
        }
        newCode
    }

    suspend fun joinByInviteCode(inviteCode: String, memberId: String): Subscription =
        withContext(Dispatchers.IO) {
            // Look up the subscription
            val sub = supabase.postgrest["subscriptions"].select {
                filter { eq("invite_code", inviteCode.trim().uppercase()) }
            }.decodeList<Subscription>().firstOrNull()
                ?: throw Exception("Invalid invite code. Please check and try again.")

            // Check expiry
            val expiresAt = sub.invite_expires_at
            if (expiresAt != null) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                try {
                    val expiry = sdf.parse(expiresAt.take(19))
                    if (expiry != null && expiry.before(java.util.Date())) {
                        throw Exception("This invite link has expired. Ask the admin for a new one.")
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("expired") == true) throw e
                }
            }

            // Check member count
            val currentCount = supabase.postgrest["subscription_members"].select {
                filter { eq("subscription_id", sub.id) }
            }.decodeList<SubscriptionMember>().size

            if (currentCount >= sub.max_members) {
                throw Exception("This group is full (${sub.max_members} members max).")
            }

            // Join
            supabase.postgrest["subscription_members"].insert(
                SubscriptionMember(
                    subscription_id = sub.id,
                    member_id = memberId,
                    role = "member"
                )
            )
            sub
        }

    suspend fun removeMemberFromSubscription(subscriptionId: String, memberId: String) =
        withContext(Dispatchers.IO) {
            supabase.postgrest["subscription_members"].delete {
                filter {
                    eq("subscription_id", subscriptionId)
                    eq("member_id", memberId)
                }
            }
        }

    suspend fun deleteSubscription(subscriptionId: String) = withContext(Dispatchers.IO) {
        supabase.postgrest["subscriptions"].delete {
            filter { eq("id", subscriptionId) }
        }
    }

    // ── Helpers ───────────────────────────────

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefix = (1..3).map { chars.random() }.joinToString("")
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "$prefix-$suffix"
    }

    private fun getExpiryTimestamp(): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.add(java.util.Calendar.HOUR, 24)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(cal.time)
    }
}
