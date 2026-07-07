package com.example.subscriptionmanager.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.data.Payment
import com.example.subscriptionmanager.ui.MemberState
import com.example.subscriptionmanager.ui.SubscriptionViewModel
import java.util.Calendar

val DarkBg = Color(0xFF0D0D0D)
val CardBg = Color(0xFF1E2740)
val AccentGreen = Color(0xFF1DB954)
val AccentRed = Color(0xFFE05252)
val AccentBlue = Color(0xFF4A9EFF)
val TextMuted = Color(0xFF8A9BB5)
val TextWhite = Color.White

@Composable
fun MemberDashboardScreen(viewModel: SubscriptionViewModel, subscriptionId: String = "", onLogout: () -> Unit, onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val activeSubscription by viewModel.activeSubscription.collectAsStateWithLifecycle()

    LaunchedEffect(subscriptionId) {
        viewModel.loadData()
        viewModel.checkUnreadNotifications()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(activeSubscription?.name ?: "Group Member", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111827), tonalElevation = 0.dp) {
                    listOf(
                        Triple(Icons.Filled.Home, "Dashboard", 0),
                        Triple(Icons.Filled.DateRange, "History", 1),
                        Triple(Icons.Filled.Settings, "Settings", 2)
                    ).forEach { (icon, label, index) ->
                        NavigationBarItem(
                            icon = { Icon(icon, null) },
                            label = { Text(label) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGreen, selectedTextColor = AccentGreen,
                                indicatorColor = Color(0xFF1A2E20)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues).fillMaxSize()) {
                if (currentUser == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                } else {
                    when (selectedTab) {
                        0 -> MemberOverviewTab(viewModel, currentUser!!)
                        1 -> MemberHistoryTab(currentUser!!)
                        2 -> AppSettingsTab(onLogout)
                    }
                }
            }
            // Notification banner
            val unreadNotifs by viewModel.unreadNotifications.collectAsStateWithLifecycle()
            if (unreadNotifs.isNotEmpty()) {
                LaunchedEffect(Unit) { viewModel.checkUnreadNotifications() }
                Box(
                    Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFF3D2A10), RoundedCornerShape(16.dp))
                        .padding(16.dp).align(Alignment.BottomCenter)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, null, tint = Color(0xFFFFA726), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Reminder from Admin", color = Color(0xFFFFA726), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(unreadNotifs.first().message, color = TextWhite, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.markNotificationsRead() }) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberOverviewTab(viewModel: SubscriptionViewModel, user: MemberState) {
    var amountText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(todayDateString()) }
    var paymentForMonth by remember { mutableStateOf(currentMonthString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val progress = if (user.totalMonthsPaid > 0) user.monthsUsed.toFloat() / user.totalMonthsPaid.toFloat() else 0f
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000), label = "p")

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        showDatePicker = false
    }

    if (showMonthPicker) {
        // Simple mock month picker since Android doesn't have a built-in one easily.
        // We'll just use a dialog with a few upcoming months.
        MonthPickerDialog(
            currentMonth = paymentForMonth,
            onMonthSelected = { paymentForMonth = it; showMonthPicker = false },
            onDismiss = { showMonthPicker = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(AccentGreen), contentAlignment = Alignment.Center) {
                    Text(user.name.first().toString(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Hello,", color = TextMuted, fontSize = 13.sp)
                    Text(user.name.split(" ").first(), fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 20.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Subscription Status", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 15.sp)
                        StatusBadge(user.status)
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Months Paid", "${user.totalMonthsPaid}", AccentGreen)
                        VerticalDivider(Modifier.height(50.dp), color = Color(0xFF2D3A55))
                        StatItem("Months Used", "${user.monthsUsed}", AccentBlue)
                        VerticalDivider(Modifier.height(50.dp), color = Color(0xFF2D3A55))
                        StatItem("Remaining", "${user.remainingBalance}", if (user.remainingBalance <= 0) AccentRed else AccentGreen)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Usage", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        color = if (animatedProgress > 0.85f) AccentRed else AccentGreen,
                        trackColor = Color(0xFF2D3A55), strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("${(animatedProgress * 100).toInt()}% used", color = TextMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("💳  Add Payment", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (Rs)", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite
                        )
                    )
                    Spacer(Modifier.height(10.dp))

                    // Date picker field
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Date", color = TextMuted) },
                        trailingIcon = {
                            Icon(Icons.Filled.DateRange, null, tint = AccentGreen,
                                modifier = Modifier.clickable { showDatePicker = true })
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            disabledTextColor = TextWhite, disabledBorderColor = Color(0xFF2D3A55),
                            disabledLabelColor = TextMuted
                        )
                    )
                    // Payment For Month field
                    OutlinedTextField(
                        value = paymentForMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment For Month", color = TextMuted) },
                        trailingIcon = {
                            Icon(Icons.Filled.List, null, tint = AccentGreen,
                                modifier = Modifier.clickable { showMonthPicker = true })
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            disabledTextColor = TextWhite, disabledBorderColor = Color(0xFF2D3A55),
                            disabledLabelColor = TextMuted
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    
                    val amountNum = amountText.toDoubleOrNull()
                    if (amountNum != null && amountNum > 212) {
                        val extraMonths = (amountNum / 212).toInt()
                        Text("💡 Rs. ${amountNum.toInt()} covers $extraMonths months.", color = AccentGreen, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (amountNum != null) { viewModel.addPayment(user.id, amountNum, selectedDate, paymentForMonth); amountText = "" }
                        },
                        enabled = amountNum != null && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        else Text("Submit Payment", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun MemberHistoryTab(user: MemberState) {
    val totalPaid = user.payments.sumOf { it.amount }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text("Payment History", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text("${user.payments.size} transaction(s) · Total Rs. ${totalPaid.toInt()}", color = TextMuted, fontSize = 13.sp)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total Paid", color = TextMuted, fontSize = 12.sp)
                        Text("Rs. ${totalPaid.toInt()}", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 22.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Months Covered", color = TextMuted, fontSize = 12.sp)
                        Text("${user.totalMonthsPaid} months", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }

        if (user.payments.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No payments yet.\nAdd your first payment!", color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(user.payments) { payment -> PaymentRow(payment) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun PaymentRow(payment: Payment) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A2E20)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Payment", fontWeight = FontWeight.SemiBold, color = TextWhite)
                Text(
                    payment.payment_for_month ?: payment.payment_date ?: payment.created_at?.take(10) ?: "Unknown date",
                    color = TextMuted, fontSize = 12.sp
                )
            }
            Text("Rs. ${payment.amount.toInt()}", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 17.sp)
        }
    }
}

@Composable
fun AppSettingsTab(onLogout: () -> Unit, viewModel: SubscriptionViewModel? = null) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    var notificationsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true)) }
    var daysBefore by remember { mutableFloatStateOf(sharedPrefs.getInt("days_before_reminder", 4).toFloat()) }
    
    val updateAvailableState = viewModel?.updateAvailable?.collectAsStateWithLifecycle()
    val updateAvailable = updateAvailableState?.value ?: false

    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "1.3"

    LaunchedEffect(Unit) {
        viewModel?.checkForUpdates(versionName)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("Settings", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 22.sp)
        Spacer(Modifier.height(24.dp))

        // Notifications Card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Notifications", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Payment Reminders", color = TextWhite, fontSize = 14.sp)
                        Text("Get notified before the 8th", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            sharedPrefs.edit().putBoolean("notifications_enabled", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = Color(0xFF1A2E20))
                    )
                }
                if (notificationsEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("Remind me ${daysBefore.toInt()} days before", color = TextMuted, fontSize = 13.sp)
                    Slider(
                        value = daysBefore,
                        onValueChange = { daysBefore = it },
                        onValueChangeFinished = { sharedPrefs.edit().putInt("days_before_reminder", daysBefore.toInt()).apply() },
                        valueRange = 1f..4f, steps = 2,
                        colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                    )
                    Text("Daily reminder starting ${daysBefore.toInt()} days before the 8th.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // About Card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 16.sp)
                AboutRow(label = "App", value = "SubManager")
                AboutRow(label = "Version", value = "v$versionName")
                AboutRow(label = "Subscription", value = "Spotify Premium")
                AboutRow(label = "Monthly Cost", value = "Rs. 212")
                AboutRow(label = "Started", value = "May 2026")
                AboutRow(label = "Payment Day", value = "8th of every month")
                AboutRow(label = "Developer", value = "DMStyles")
            }
        }

        // Check for Updates Card
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var checkResult by remember { mutableStateOf<String?>(null) }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Updates", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Current Version", color = TextMuted, fontSize = 12.sp)
                        Text("v$versionName", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (updateAvailable) {
                        Button(
                            onClick = {
                                val url = viewModel?.latestUpdateUrl?.value
                                if (url != null) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Update Available!", fontSize = 12.sp) }
                    } else {
                        Box(Modifier.background(Color(0xFF1A2E20), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text("Up to date ✓", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Manual check button
                OutlinedButton(
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            checkResult = null
                            viewModel?.checkForUpdates(versionName) { hasUpdate ->
                                isCheckingUpdate = false
                                checkResult = if (hasUpdate) "Update found! Tap above to download." else "You're on the latest version!"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4A5A))
                ) {
                    if (isCheckingUpdate) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentGreen, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Checking...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check for Updates", fontSize = 13.sp)
                    }
                }
                if (checkResult != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(checkResult!!, color = AccentGreen, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Logout
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentRed)
            ) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(value, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 24.sp)
        Text(label, color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when {
        status == "ACTIVE"             -> Triple(Color(0xFF1A2E20), AccentGreen, "✓ Active")
        status == "PAYMENT DUE"        -> Triple(Color(0xFF3D1A1A), AccentRed, "⚠ Payment Due")
        status == "PAYMENT DUE TODAY"  -> Triple(Color(0xFF3D2A10), Color(0xFFFFA726), "⏰ Due Today")
        status.startsWith("DUE IN")    -> Triple(Color(0xFF3D2A10), Color(0xFFFFA726), "⏰ $status")
        else                           -> Triple(Color(0xFF1A2E20), AccentGreen, "✓ Active")
    }
    Box(
        modifier = Modifier.background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun todayDateString(): String {
    val cal = Calendar.getInstance()
    return "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

fun currentMonthString(): String {
    val cal = Calendar.getInstance()
    return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

@Composable
fun MonthPickerDialog(currentMonth: String, onMonthSelected: (String) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Select Month", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, Calendar.APRIL) // Start from April
                // Generate 12 months starting from April
                for (i in 0..11) {
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1
                    val monthStr = "%04d-%02d".format(year, month)
                    val displayStr = "${java.text.DateFormatSymbols().months[month - 1]} $year"
                    
                    Text(
                        text = displayStr,
                        color = if (monthStr == currentMonth) AccentGreen else TextWhite,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onMonthSelected(monthStr) }.padding(vertical = 12.dp)
                    )
                    cal.add(Calendar.MONTH, 1)
                }
                
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = TextMuted)
                }
            }
        }
    }
}
