package com.example.subscriptionmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscriptionmanager.BuildConfig
import com.example.subscriptionmanager.data.*
import com.example.subscriptionmanager.service.UpdateChecker
import com.example.subscriptionmanager.service.UpdateInfo
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// UI State Models
// ─────────────────────────────────────────────

data class MemberState(
    val id: String,
    val name: String,
    val role: String,
    val totalMonthsPaid: Int,
    val monthsUsed: Int,
    val remainingBalance: Int,
    val status: String,
    val payments: List<Payment>
)

data class SubscriptionWithRole(
    val subscription: Subscription,
    val myRole: String,       // "admin" or "member"
    val myMemberState: MemberState?
)

// ─────────────────────────────────────────────
// Navigation Routes
// ─────────────────────────────────────────────

sealed class AppRoute {
    object Loading : AppRoute()
    object Login : AppRoute()
    object ClaimProfile : AppRoute()
    object SubscriptionList : AppRoute()
    data class Dashboard(val subscriptionId: String, val role: String) : AppRoute()
    data class MemberDetail(val memberId: String, val subscriptionId: String) : AppRoute()
    data class CreateSubscription(val prefillInviteCode: String? = null) : AppRoute()
    object JoinSubscription : AppRoute()
    data class EditSubscription(val subscriptionId: String) : AppRoute()
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

class SubscriptionViewModel : ViewModel() {
    private val repository = SubscriptionRepository()
    private val supabase = com.example.subscriptionmanager.data.SupabaseApi.client

    val route = MutableStateFlow<AppRoute>(AppRoute.Loading)

    // Current user
    private val _currentMember = MutableStateFlow<Member?>(null)
    val currentMember: StateFlow<Member?> = _currentMember

    // Subscription list (home screen)
    private val _subscriptions = MutableStateFlow<List<SubscriptionWithRole>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionWithRole>> = _subscriptions

    // Active subscription context
    private val _activeSubscription = MutableStateFlow<Subscription?>(null)
    val activeSubscription: StateFlow<Subscription?> = _activeSubscription

    // Members in the active subscription (manager view)
    private val _members = MutableStateFlow<List<MemberState>>(emptyList())
    val members: StateFlow<List<MemberState>> = _members

    // Current user state within active subscription
    private val _currentUserState = MutableStateFlow<MemberState?>(null)
    val currentUserState: StateFlow<MemberState?> = _currentUserState

    // Unclaimed members (for claim profile screen)
    private val _unclaimedMembers = MutableStateFlow<List<Member>>(emptyList())
    val unclaimedMembers: StateFlow<List<Member>> = _unclaimedMembers

    // Invite code for sharing
    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _unreadNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val unreadNotifications: StateFlow<List<AppNotification>> = _unreadNotifications

    // Update checker
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    fun checkForUpdate() {
        viewModelScope.launch {
            val info = UpdateChecker.check(BuildConfig.VERSION_NAME)
            if (info.available) _updateInfo.value = info
        }
    }

    fun dismissUpdate() { _updateInfo.value = null }

    // ── Helpers ───────────────────────────────

    fun navigateTo(r: AppRoute) { route.value = r }
    fun clearError() { _errorMessage.value = null }
    fun clearSuccess() { _successMessage.value = null }
    fun clearPingSuccess() { _successMessage.value = null }

    // ── Auth & Routing ─────────────────────────

    fun checkAuthAndRoute() {
        viewModelScope.launch {
            route.value = AppRoute.Loading
            try {
                val authUser = supabase.auth.currentUserOrNull()
                if (authUser == null) {
                    route.value = AppRoute.Login
                    return@launch
                }
                val member = repository.getMemberByAuthId(authUser.id)
                if (member == null) {
                    _unclaimedMembers.value = repository.getUnclaimedMembers()
                    route.value = AppRoute.ClaimProfile
                } else {
                    _currentMember.value = member
                    loadSubscriptionList(member)
                }
            } catch (e: Exception) {
                route.value = AppRoute.Login
            }
        }
    }

    fun claimProfile(memberId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                repository.claimProfile(memberId, authUser.id)
                val member = repository.getMemberByAuthId(authUser.id)
                if (member != null) {
                    _currentMember.value = member
                    loadSubscriptionList(member)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to claim profile. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (_: Exception) {}
            _currentMember.value = null
            _subscriptions.value = emptyList()
            _members.value = emptyList()
            _currentUserState.value = null
            _activeSubscription.value = null
            route.value = AppRoute.Login
            onComplete()
        }
    }

    // ── Subscription List ─────────────────────

    fun loadSubscriptionList(member: Member? = null) {
        viewModelScope.launch {
            try {
                val m = member ?: _currentMember.value ?: return@launch
                val subs = repository.getSubscriptionsForMember(m.id)

                if (subs.isEmpty()) {
                    _subscriptions.value = emptyList()
                    route.value = AppRoute.SubscriptionList
                    return@launch
                }

                val cal = java.util.Calendar.getInstance()
                val withRoles = subs.map { sub ->
                    val role = repository.getRoleInSubscription(sub.id, m.id)
                    val payments = repository.getPaymentsForUser(m.id, sub.id)
                    val myState = computeMemberState(m, payments, sub, cal)
                    SubscriptionWithRole(sub, role, myState)
                }
                _subscriptions.value = withRoles
                route.value = AppRoute.SubscriptionList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load subscriptions."
                e.printStackTrace()
            }
        }
    }

    // ── Active Subscription / Dashboard ───────

    fun openSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val member = _currentMember.value ?: return@launch
                val sub = repository.getSubscriptionById(subscriptionId) ?: return@launch
                _activeSubscription.value = sub
                _inviteCode.value = sub.invite_code
                loadData(sub)
                val role = repository.getRoleInSubscription(subscriptionId, member.id)
                route.value = AppRoute.Dashboard(subscriptionId, role)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to open subscription."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadData(sub: Subscription? = null) {
        viewModelScope.launch {
            try {
                val subscription = sub ?: _activeSubscription.value ?: return@launch
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val dbMembers = repository.getMembersForSubscription(subscription.id)
                val cal = java.util.Calendar.getInstance()

                val currentMember = dbMembers.find { it.auth_id == authUser.id }
                if (currentMember != null) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                viewModelScope.launch {
                                    repository.updateFcmToken(currentMember.id, task.result)
                                }
                            }
                        }
                }

                val memberStates = dbMembers.map { member ->
                    val payments = repository.getPaymentsForUser(member.id, subscription.id)
                    computeMemberState(member, payments, subscription, cal)
                }

                _members.value = memberStates
                _currentUserState.value = memberStates.find { state ->
                    dbMembers.find { it.id == state.id }?.auth_id == authUser.id
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load data."
                e.printStackTrace()
            }
        }
    }

    private fun computeMemberState(
        member: Member,
        payments: List<Payment>,
        sub: Subscription,
        cal: java.util.Calendar
    ): MemberState {
        val totalPaidAmount = payments.sumOf { it.amount }
        val monthlyCost = sub.monthly_cost
        val totalMonthsPaid = (totalPaidAmount / monthlyCost).toInt()

        // Parse subscription start date
        val parts = sub.start_date.split("-")
        val startYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val startMonth = parts.getOrNull(1)?.toIntOrNull() ?: 5

        val currentYear = cal.get(java.util.Calendar.YEAR)
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
        var dynamicMonthsUsed = (currentYear - startYear) * 12 + (currentMonth - startMonth) + 1
        if (dynamicMonthsUsed < 1) dynamicMonthsUsed = 1

        val remainingBalance = totalMonthsPaid - dynamicMonthsUsed
        val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val billingDay = sub.billing_day

        val status = when {
            remainingBalance >= 0 -> "ACTIVE"
            remainingBalance == -1 && today <= billingDay -> {
                val daysLeft = billingDay - today
                if (daysLeft == 0) "PAYMENT DUE TODAY" else "DUE IN $daysLeft DAYS"
            }
            else -> "PAYMENT DUE"
        }

        return MemberState(
            id = member.id,
            name = member.name,
            role = member.role,
            totalMonthsPaid = totalMonthsPaid,
            monthsUsed = dynamicMonthsUsed,
            remainingBalance = remainingBalance,
            status = status,
            payments = payments.sortedByDescending { it.created_at }
        )
    }

    // ── Payments ──────────────────────────────

    fun addPayment(memberId: String, amount: Double, paymentDate: String, paymentForMonth: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sub = _activeSubscription.value
                val cost = sub?.monthly_cost ?: 211.0
                val monthsCount = (amount / cost).toInt().coerceAtLeast(1)
                val parts = paymentForMonth.split("-")

                if (monthsCount > 1 && parts.size == 2) {
                    var year = parts[0].toIntOrNull() ?: 2026
                    var month = parts[1].toIntOrNull() ?: 6
                    val amountPerMonth = amount / monthsCount
                    for (i in 0 until monthsCount) {
                        val currentMonthStr = "$year-${String.format("%02d", month)}"
                        repository.addPayment(memberId, amountPerMonth, paymentDate, currentMonthStr, sub?.id)
                        month++
                        if (month > 12) { month = 1; year++ }
                    }
                } else {
                    repository.addPayment(memberId, amount, paymentDate, paymentForMonth, sub?.id)
                }
                loadData()
                loadSubscriptionList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add payment."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deletePayment(paymentId)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete payment."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Members in subscription ───────────────

    fun addMember(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addMember(name)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add member."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val subId = _activeSubscription.value?.id
                if (subId != null) {
                    repository.removeMemberFromSubscription(subId, memberId)
                }
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove member."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pingMember(memberId: String, memberName: String) {
        viewModelScope.launch {
            try {
                val sub = _activeSubscription.value ?: run {
                    _errorMessage.value = "No active group found."
                    return@launch
                }
                val cost = sub.monthly_cost.toInt()
                val billingDay = sub.billing_day
                repository.sendPing(memberId, sub.id, "Reminder: Your payment of Rs. $cost is due on the ${billingDay}th!")
                _successMessage.value = "Pinged $memberName!"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send ping."
            }
        }
    }

    fun incrementMonthsUsedForEveryone() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.incrementMonthsUsedForEveryone()
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to increment months."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Notifications ─────────────────────────

    fun checkUnreadNotifications() {
        viewModelScope.launch {
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val member = repository.getMemberByAuthId(authUser.id) ?: return@launch
                val sub = _activeSubscription.value ?: return@launch
                _unreadNotifications.value = repository.getUnreadNotifications(member.id, sub.id)
            } catch (_: Exception) {}
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val member = repository.getMemberByAuthId(authUser.id) ?: return@launch
                val sub = _activeSubscription.value ?: return@launch
                repository.markNotificationsRead(member.id, sub.id)
                _unreadNotifications.value = emptyList()
            } catch (_: Exception) {}
        }
    }

    // ── Create / Join / Edit Subscription ─────

    fun createSubscription(
        name: String,
        description: String?,
        monthlyCost: Double,
        billingDay: Int,
        startDate: String,
        maxMembers: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val member = _currentMember.value ?: return@launch

                val created = repository.createSubscription(
                    name = name,
                    description = description,
                    monthlyCost = monthlyCost,
                    billingDay = billingDay,
                    startDate = startDate,
                    maxMembers = maxMembers,
                    adminId = authUser.id,
                    creatorMemberId = member.id
                )
                _activeSubscription.value = created
                _inviteCode.value = created.invite_code
                loadSubscriptionList()
                route.value = AppRoute.Dashboard(created.id, "admin")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create subscription: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinSubscription(inviteCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val member = _currentMember.value ?: return@launch
                val sub = repository.joinByInviteCode(inviteCode, member.id)
                _activeSubscription.value = sub
                loadSubscriptionList()
                route.value = AppRoute.Dashboard(sub.id, "member")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to join subscription."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSubscription(
        subscriptionId: String,
        name: String,
        description: String?,
        monthlyCost: Double,
        billingDay: Int,
        startDate: String,
        maxMembers: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateSubscription(subscriptionId, name, description, monthlyCost, billingDay, startDate, maxMembers)
                val updated = repository.getSubscriptionById(subscriptionId)
                _activeSubscription.value = updated
                _successMessage.value = "Subscription updated!"
                loadSubscriptionList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshInviteCode(subscriptionId: String) {
        viewModelScope.launch {
            try {
                val newCode = repository.refreshInviteCode(subscriptionId)
                _inviteCode.value = newCode
                _successMessage.value = "New invite code generated!"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh invite code."
            }
        }
    }

    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteSubscription(subscriptionId)
                _activeSubscription.value = null
                _members.value = emptyList()
                // Navigate back to list first, then reload
                route.value = AppRoute.SubscriptionList
                loadSubscriptionList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

}

