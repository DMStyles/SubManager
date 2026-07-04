package com.example.subscriptionmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscriptionmanager.data.AppNotification
import com.example.subscriptionmanager.data.Member
import com.example.subscriptionmanager.data.Payment
import com.example.subscriptionmanager.data.SubscriptionRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

sealed class AppRoute {
    object Loading : AppRoute()
    object Login : AppRoute()
    object ClaimProfile : AppRoute()
    data class Dashboard(val role: String) : AppRoute()
    data class MemberDetail(val memberId: String) : AppRoute()
}

class SubscriptionViewModel : ViewModel() {
    private val repository = SubscriptionRepository()
    private val supabase = com.example.subscriptionmanager.data.SupabaseApi.client

    private val _route = MutableStateFlow<AppRoute>(AppRoute.Loading)
    val route: StateFlow<AppRoute> = _route

    private val _members = MutableStateFlow<List<MemberState>>(emptyList())
    val members: StateFlow<List<MemberState>> = _members

    private val _currentUserState = MutableStateFlow<MemberState?>(null)
    val currentUserState: StateFlow<MemberState?> = _currentUserState

    private val _unclaimedMembers = MutableStateFlow<List<Member>>(emptyList())
    val unclaimedMembers: StateFlow<List<Member>> = _unclaimedMembers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _unreadNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val unreadNotifications: StateFlow<List<AppNotification>> = _unreadNotifications

    private val _pingSuccess = MutableStateFlow<String?>(null)
    val pingSuccess: StateFlow<String?> = _pingSuccess

    fun navigateTo(appRoute: AppRoute) {
        _route.value = appRoute
    }

    fun checkAuthAndRoute() {
        viewModelScope.launch {
            _route.value = AppRoute.Loading
            try {
                val authUser = supabase.auth.currentUserOrNull()
                if (authUser == null) {
                    _route.value = AppRoute.Login
                    return@launch
                }
                val member = repository.getMemberByAuthId(authUser.id)
                if (member == null) {
                    _unclaimedMembers.value = repository.getUnclaimedMembers()
                    _route.value = AppRoute.ClaimProfile
                } else {
                    _route.value = AppRoute.Dashboard(member.role)
                    loadData()
                }
            } catch (e: Exception) {
                _route.value = AppRoute.Login
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
                    _route.value = AppRoute.Dashboard(member.role)
                    loadData()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to claim profile. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val dbMembers = repository.getMembers()

                val cal = java.util.Calendar.getInstance()
                val currentYear = cal.get(java.util.Calendar.YEAR)
                val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
                
                val currentMember = dbMembers.find { it.auth_id == authUser.id }
                if (currentMember != null) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            viewModelScope.launch {
                                repository.updateFcmToken(currentMember.id, task.result)
                            }
                        }
                    }
                }

                var dynamicMonthsUsed = (currentYear - 2026) * 12 + (currentMonth - 5) + 1 // May 2026
                if (dynamicMonthsUsed < 1) dynamicMonthsUsed = 1

                val memberStates = dbMembers.map { member ->
                    val payments = repository.getPaymentsForUser(member.id)
                    val totalPaidAmount = payments.sumOf { it.amount }
                    val totalMonthsPaid = (totalPaidAmount / 211.0).toInt()
                    val remainingBalance = totalMonthsPaid - dynamicMonthsUsed

                    // Calculate days until the 8th of next month (payment day)
                    val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val daysUntilPaymentDay = if (today <= 8) {
                        8 - today  // days until 8th of this month
                    } else {
                        // Days left in this month + 8 days into next month
                        val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        (maxDay - today) + 8
                    }

                    val status = when {
                        remainingBalance > 0 -> "ACTIVE" // paid ahead
                        remainingBalance == 0 && daysUntilPaymentDay > 4 -> "ACTIVE" // paid this month, payment not close
                        remainingBalance == 0 && daysUntilPaymentDay in 1..4 -> "DUE IN $daysUntilPaymentDay DAYS"
                        remainingBalance == 0 && daysUntilPaymentDay == 0 -> "PAYMENT DUE TODAY"
                        else -> "PAYMENT DUE" // overdue
                    }

                    MemberState(
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

    fun addPayment(memberId: String, amount: Double, paymentDate: String, paymentForMonth: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val monthsCount = (amount / 211.0).toInt().coerceAtLeast(1)
                val parts = paymentForMonth.split("-")
                
                if (monthsCount > 1 && parts.size == 2) {
                    var year = parts[0].toIntOrNull() ?: 2026
                    var month = parts[1].toIntOrNull() ?: 6
                    val amountPerMonth = amount / monthsCount
                    
                    for (i in 0 until monthsCount) {
                        val currentMonthStr = "$year-${String.format("%02d", month)}"
                        repository.addPayment(memberId, amountPerMonth, paymentDate, currentMonthStr)
                        month++
                        if (month > 12) {
                            month = 1
                            year++
                        }
                    }
                } else {
                    repository.addPayment(memberId, amount, paymentDate, paymentForMonth)
                }
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add payment."
            } finally {
                _isLoading.value = false
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
                repository.deleteMember(memberId)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete member."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
            } catch (_: Exception) {}
            _currentUserState.value = null
            _members.value = emptyList()
            _route.value = AppRoute.Login
            onComplete()
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

    fun pingMember(memberId: String, memberName: String) {
        viewModelScope.launch {
            try {
                repository.sendPing(memberId, "Reminder: Your Spotify payment of Rs. 212 is due on the 8th!")
                _pingSuccess.value = "Pinged $memberName!"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send ping."
            }
        }
    }

    fun checkUnreadNotifications() {
        viewModelScope.launch {
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val member = repository.getMemberByAuthId(authUser.id) ?: return@launch
                _unreadNotifications.value = repository.getUnreadNotifications(member.id)
            } catch (_: Exception) {}
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            try {
                val authUser = supabase.auth.currentUserOrNull() ?: return@launch
                val member = repository.getMemberByAuthId(authUser.id) ?: return@launch
                repository.markNotificationsRead(member.id)
                _unreadNotifications.value = emptyList()
            } catch (_: Exception) {}
        }
    }

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    private val _latestUpdateUrl = MutableStateFlow<String?>(null)
    val latestUpdateUrl: StateFlow<String?> = _latestUpdateUrl

    fun checkForUpdates(currentVersion: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://raw.githubusercontent.com/DMStyles/SubManager/master/version.json")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val latestVersion = json.getString("versionName")
                    val downloadUrl = json.getString("downloadUrl")
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (latestVersion != currentVersion) {
                            _updateAvailable.value = true
                            _latestUpdateUrl.value = downloadUrl
                            onResult?.invoke(true)
                        } else {
                            onResult?.invoke(false)
                        }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onResult?.invoke(false) }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onResult?.invoke(false) }
            }
        }
    }

    fun clearPingSuccess() { _pingSuccess.value = null }
    fun clearError() { _errorMessage.value = null }
}
